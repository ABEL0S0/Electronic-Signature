import sys
import traceback

from pyhanko.sign import signers
from pyhanko.stamp import TextStampStyle  # Puedes cambiar a QRStampStyle si deseas
from pyhanko.sign.fields import SigFieldSpec
from pyhanko.sign.signers.pdf_signer import PdfSigner, PdfSignatureMetadata
from pyhanko_certvalidator import ValidationContext
from pyhanko.sign.validation import validate_pdf_signature
from pyhanko.pdf_utils.incremental_writer import IncrementalPdfFileWriter

print("[firmar-pdf.py] Script iniciado.")

def firmar_pdf(cert_path, cert_password, pdf_input, pdf_output, page, x1, y1, x2, y2):
    print(f"[firmar-pdf.py] firmar_pdf() llamado con:\n  cert_path={cert_path}\n  pdf_input={pdf_input}\n  pdf_output={pdf_output}\n  page={page}, x1={x1}, y1={y1}, x2={x2}, y2={y2}")

    try:
        signer = signers.SimpleSigner.load_pkcs12(cert_path, cert_password.encode())
        print("[firmar-pdf.py] Certificado cargado correctamente.")
    except Exception as e:
        print(f"[firmar-pdf.py] Error cargando certificado: {e}")
        traceback.print_exc()
        raise

    # Estilo visible del sello (puede cambiarse por QRStampStyle si se prefiere)
    style = TextStampStyle(stamp_text="Firmado digitalmente por {signer}", background=None)

    try:
        with open(pdf_input, "rb") as inf, open(pdf_output, "wb") as outf:
            w = IncrementalPdfFileWriter(inf)

            # Verificar si el campo de firma ya existe
            #if w.find_sig_field("Sig1") is not None:
            #    print("[firmar-pdf.py] El campo de firma 'Sig1' ya existe en el PDF. Elige otro nombre o elimina el campo existente.")
            #    return

            meta = PdfSignatureMetadata(field_name="Sig1")

            signer_obj = PdfSigner(
                signature_meta=meta,
                signer=signer,
                stamp_style=style,
                new_field_spec=SigFieldSpec(
                    sig_field_name="Sig1",
                    on_page=int(page),
                    box=(float(x1), float(y1), float(x2), float(y2))
                )
            )

            # Quitar with_validation_context, no es necesario
            # vc = ValidationContext(allow_fetching=True)
            # signer_obj = with_validation_context(signer_obj, vc)

            signer_obj.sign_pdf(w, output=outf)
            print("[firmar-pdf.py] PDF firmado correctamente.")

    except Exception as e:
        print(f"[firmar-pdf.py] Error firmando PDF: {e}")
        traceback.print_exc()
        raise

def verificar_pdf(pdf_output):
    print(f"[firmar-pdf.py] Verificando firma de: {pdf_output}")
    try:
        with open(pdf_output, "rb") as f:
            result = validate_pdf_signature(f, "Sig1", ValidationContext(allow_fetching=True))
            print("[firmar-pdf.py] Resultado de validación:")
            print(result.pretty_print_details())
    except Exception as e:
        print(f"[firmar-pdf.py] Error validando PDF: {e}")
        traceback.print_exc()

if __name__ == "__main__":
    print(f"[firmar-pdf.py] Argumentos recibidos: {sys.argv}")
    if len(sys.argv) != 10:
        print("Uso: python firmar-pdf.py cert.p12 password input.pdf output.pdf page x1 y1 x2 y2")
        sys.exit(1)

    try:
        firmar_pdf(*sys.argv[1:])
        verificar_pdf(sys.argv[4])
    except Exception:
        print("[firmar-pdf.py] Falló la firma o verificación del PDF.")
        sys.exit(1)