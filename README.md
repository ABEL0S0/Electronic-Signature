# Electronic Signature - README

## Descripción General

**Electronic Signature** es una plataforma web para la gestión, firma electrónica y flujo de aprobación de documentos PDF. Permite a los usuarios subir documentos, solicitar firmas a otros usuarios, firmar digitalmente con certificados, y recibir notificaciones en tiempo real mediante WebSockets.

El sistema está compuesto por:
- **Frontend**: SPA en Vue.js + TailwindCSS
- **Backend**: Java Spring Boot, integración con MongoDB y base de datos relacional (JPA/Hibernate)
- **Firmado**: Integración con PyHanko (Python) para la firma digital de PDFs
- **WebSockets**: Notificaciones y solicitudes de firma en tiempo real

---

## Estructura de Carpetas

```
Electronic-Signature/
├── despliegue/           # Archivos de despliegue (Docker, nginx, scripts)
├── ES-Backend/           # Backend Java Spring Boot
│   ├── src/
│   │   ├── main/java/com/ES/Backend/
│   │   │   ├── controller/         # Controladores REST y WebSocket
│   │   │   ├── entity/             # Entidades JPA y MongoDB
│   │   │   ├── repository/         # Repositorios JPA/Mongo
│   │   │   ├── service/            # Lógica de negocio
│   │   │   └── signer/             # Scripts y recursos de firma (PyHanko)
│   │   └── resources/
│   └── files/uploads/    # Almacenamiento de PDFs y temporales
├── ES-Frontend/          # Frontend Vue.js
│   ├── src/
│   │   ├── components/   # Componentes Vue
│   │   ├── pages/        # Vistas principales
│   │   ├── service/      # Servicios de API y WebSocket
│   │   └── utils/        # Utilidades
└── ...
```

---

## Topología de Bases de Datos

### MongoDB (NoSQL)
- **DocumentMetadata**:  
  - id (ObjectId)
  - user (String, email)
  - fileName (String)
  - filePath (String, ruta absoluta/relativa)
  - isSigned (boolean)
  - uploadedAt (datetime)

### Relacional (JPA/Hibernate, por ejemplo MySQL/Postgres)
- **SignatureRequest**:
  - id (Long, autoincrement)
  - documentPath (String, ruta del archivo PDF)
  - status (String: PENDIENTE, COMPLETADO)
  - createdAt (datetime)
  - users (List<SignatureRequestUser>)

- **SignatureRequestUser**:
  - id (Long)
  - userId (Long)
  - page, posX, posY (int)
  - status (String: PENDIENTE, PERMITIDO, DENEGADO)
  - certificateId (String)
  - certificatePassword (String)
  - signatureRequest (SignatureRequest, FK)
  - respondedAt (datetime)

- **User** y **Certificate**:  
  - Gestión de usuarios y certificados digitales

---

## Backend (Spring Boot)

### Principales Controladores REST

#### `/api/documents`
- **POST `/upload`**  
  Sube un documento PDF.  
  _Body_: `file` (multipart)  
  _Auth_: Bearer Token  
  _Response_: `DocumentMetadata`

- **GET `/list`**  
  Lista los documentos del usuario autenticado.  
  _Auth_: Bearer Token  
  _Response_: `[DocumentMetadata]`

- **GET `/download/{id}`**  
  Descarga un documento por ID.  
  _Auth_: Bearer Token  
  _Response_: PDF

- **GET `/{id}/view`**  
  Visualiza un documento PDF en línea.  
  _Response_: PDF

- **GET `/search?filepath=...`**  
  Busca un documento por ruta absoluta/relativa.  
  _Response_: `DocumentMetadata` o 404

- **POST `/sign/{id}`**  
  Firma un documento PDF con certificado.  
  _Body_:  
    - page, x, y, certificateId, certPassword  
  _Auth_: Bearer Token  
  _Response_: PDF firmado

#### `/api/signature-requests`
- **POST `/`**  
  Crea una solicitud de firma grupal.  
  _Body_:  
    - documentPath (ruta del PDF)
    - users: [{userId, page, posX, posY}]
  _Response_: `SignatureRequest`

- **GET `/`**  
  Lista todas las solicitudes de firma.

- **POST `/respond`**  
  Responde a una solicitud de firma (permitir/denegar).

---

### WebSocket Endpoints

- **`/ws`**  
  Conexión WebSocket para notificaciones y solicitudes en tiempo real.

  **Mensajes enviados:**
  - `SIGNATURE_REQUEST`: Nueva solicitud de firma para un usuario
  - `SIGNATURE_REQUEST_UPDATE`: Actualización de estado de una solicitud
  - `NOTIFICATION`: Notificaciones generales

  **Mensajes recibidos:**
  - Respuestas a solicitudes de firma

---

## Frontend (Vue.js)

### Principales Componentes

- **Login/Register/Verification**: Autenticación y registro de usuarios
- **Dashboard**: Panel principal con documentos y solicitudes
- **SignDocuments.vue**: Visualización y firma de documentos
- **PendingSignatureRequests.vue**: Solicitudes de firma pendientes (con visualizador PDF y posición de firma)
- **RequestGroupSignature.vue**: Solicitar firmas grupales a varios usuarios
- **NotificationList.vue**: Notificaciones en tiempo real

### Servicios

- **api.ts**: Llamadas a la API REST
- **WebSocketService.ts**: Gestión de WebSocket y listeners
- **Auth.ts**: Estado de autenticación

---

## Flujo de Trabajo Principal

1. **Subida de documento**:  
   El usuario sube un PDF, que se almacena y se registra en MongoDB.

2. **Solicitud de firma**:  
   El usuario selecciona un documento y solicita firmas a otros usuarios, indicando la posición de la firma.

3. **Notificación WebSocket**:  
   Los usuarios reciben una notificación en tiempo real de la solicitud de firma.

4. **Visualización y respuesta**:  
   El usuario ve el documento, la posición de la firma y responde (permitir/denegar).

5. **Firma digital**:  
   Si todos permiten, el backend ejecuta PyHanko para firmar el PDF con los certificados.

6. **Descarga y auditoría**:  
   El documento firmado queda disponible para descarga y auditoría.

---

## Despliegue

- **Docker**:  
  Usa los archivos en `/despliegue` para levantar el stack completo (backend, frontend, nginx, MongoDB, etc).
- **Variables de entorno**:  
  Configura las variables necesarias en `env.example` y `application.properties`.
- **Certificados**:  
  Los certificados digitales se almacenan cifrados y se desencriptan solo para el proceso de firma.

---

## Notas Técnicas

- **Compatibilidad multiplataforma**:  
  El sistema maneja rutas de archivos tanto en Windows como en Linux.
- **Seguridad**:  
  Todas las operaciones críticas requieren autenticación JWT.
- **Integración con PyHanko**:  
  El backend ejecuta scripts Python para la firma digital real de PDFs.

---

## Ejemplo de Solicitud de Firma (JSON)

```json
{
  "documentPath": "files/uploads/documents/64e1b2c1e4b0a2f8b7c8d9e0/document.pdf",
  "users": [
    {
      "userId": 123,
      "page": 1,
      "posX": 100,
      "posY": 200
    }
  ]
}
```

---

## Contacto y soporte

Para dudas, sugerencias o soporte, contacta al equipo de desarrollo o abre un issue en el repositorio.
