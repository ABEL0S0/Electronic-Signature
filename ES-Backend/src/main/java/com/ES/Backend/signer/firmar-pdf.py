import os
import sys
import traceback

from pyhanko.sign import signers
from pyhanko.sign.signers.pdf_signer import PdfSigner, PdfSignatureMetadata
from pyhanko.sign.fields import SigFieldSpec
from pyhanko.stamp import QRStampStyle
from pyhanko.pdf_utils.incremental_writer import IncrementalPdfFileWriter

def firmar_pdf(cert_path, cert_password, pdf_input, pdf_output,
               page, x1, y1, x2, y2):
    print(f">> firmar_pdf: cert={cert_path}")
    print(">> Tamaño .p12:", os.path.getsize(cert_path), "bytes")

    ca_cert = os.path.join(os.path.dirname(cert_path), "ca-cert.pem")
    print(f">> CA PEM: {ca_cert} (existe? {os.path.exists(ca_cert)})")

    try:
        signer = signers.SimpleSigner.load_pkcs12(
            pfx_file=cert_path,
            ca_chain_files=[ca_cert],
            passphrase=cert_password.encode()
        )
        print(">> SimpleSigner.load_pkcs12: OK")
    except Exception as e:
        print(">> ERROR cargando P12:", e)
        traceback.print_exc()
        sys.exit(1)

    # --- EXTRAEMOS DATOS PARA EL QR ---
    cert = signer.signing_cert
    subj = getattr(cert.subject, 'native', {})
    cn    = subj.get("common_name", "")
    email = subj.get("email_address", "")
    org   = subj.get("organization_name", "")
    qr_data = f"Name: {cn}\nEmail: {email}\nOrganization: {org}"
    print(">> QR data:", repr(qr_data))

    # Creamos el estilo QR (sin payload)
    style = QRStampStyle()

    try:
        with open(pdf_input, "rb") as inf, open(pdf_output, "wb") as outf:
            w = IncrementalPdfFileWriter(inf)
            print(">> PDF entrada:", pdf_input, "tamaño:", os.path.getsize(pdf_input), "bytes")

            # Sólo metadata básica aquí
            meta = PdfSignatureMetadata(field_name="Sig1")

            pdf_signer = PdfSigner(
                signature_meta=meta,
                signer=signer,
                stamp_style=style,
                new_field_spec=SigFieldSpec(
                    sig_field_name="Sig1",
                    on_page=int(page),
                    box=(float(x1), float(y1), float(x2), float(y2))
                )
            )

            # Pasamos el QR payload como 'url' en appearance_text_params
            pdf_signer.sign_pdf(
                w, output=outf,
                appearance_text_params={"url": qr_data}
            )

        print(">> PDF firmado escrito:", pdf_output, "tamaño:", os.path.getsize(pdf_output), "bytes")
        with open(pdf_output, "rb") as f:
            print(">> Cabecera firmada:", f.read(8))

    except Exception as e:
        print(">> ERROR firmando PDF:", e)
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    if len(sys.argv) != 10:
        print("Uso: python firmar-pdf.py cert.p12 password input.pdf output.pdf page x1 y1 x2 y2")
        sys.exit(1)
    _, cert_path, cert_password, pdf_input, pdf_output, page, x1, y1, x2, y2 = sys.argv
    firmar_pdf(cert_path, cert_password, pdf_input, pdf_output, page, x1, y1, x2, y2)
    print(">> Exit OK")
    sys.exit(0)
