/**
 * Utilidades para conversiones de coordenadas y páginas para PyHanko
 * Basado en las conversiones implementadas en SignDocuments.vue
 */

/**
 * Convertir página de 1-based (frontend) a 0-based (backend)
 * @param page - Página en formato frontend (1, 2, 3...)
 * @returns Página en formato backend (0, 1, 2...)
 */
export const convertToBackendPage = (page: number): number => page - 1;

/**
 * Convertir página de 0-based (backend) a 1-based (frontend)
 * @param page - Página en formato backend (0, 1, 2...)
 * @returns Página en formato frontend (1, 2, 3...)
 */
export const convertToFrontendPage = (page: number): number => page + 1;

/**
 * Convertir coordenadas del canvas al sistema PDF estándar
 * @param canvasX - Coordenada X del canvas
 * @param canvasY - Coordenada Y del canvas
 * @param canvasWidth - Ancho del canvas
 * @param canvasHeight - Alto del canvas
 * @param pageWidth - Ancho de la página PDF
 * @param pageHeight - Alto de la página PDF
 * @returns Coordenadas en formato PDF estándar
 */
export const convertCanvasToPDFCoordinates = (
  canvasX: number, 
  canvasY: number, 
  canvasWidth: number, 
  canvasHeight: number, 
  pageWidth: number, 
  pageHeight: number
) => {
  // Convertir coordenadas del canvas (0,0 en esquina superior izquierda, Y hacia abajo)
  // al sistema PDF (0,0 en esquina inferior izquierda, Y hacia arriba)
  
  // X: se mantiene igual (crece hacia la derecha en ambos sistemas)
  const pdfX = (canvasX / canvasWidth) * pageWidth;
  
  // Y: invertir el eje Y (canvas Y hacia abajo -> PDF Y hacia arriba)
  const pdfY = pageHeight - ((canvasY / canvasHeight) * pageHeight);
  
  return { x: Math.round(pdfX), y: Math.round(pdfY) };
};

/**
 * Calcular las coordenadas de la caja de firma para PyHanko
 * @param centerX - Coordenada X del centro de la firma
 * @param centerY - Coordenada Y del centro de la firma
 * @param signatureWidth - Ancho de la firma (por defecto 120)
 * @param signatureHeight - Alto de la firma (por defecto 60)
 * @returns Objeto con coordenadas de la caja de firma (x1, y1, x2, y2)
 */
export const calculateSignatureBox = (
  centerX: number, 
  centerY: number, 
  signatureWidth: number = 120, 
  signatureHeight: number = 60
) => {
  // PyHanko usa box = (x1, y1, x2, y2) donde:
  // (x1, y1) = esquina inferior izquierda
  // (x2, y2) = esquina superior derecha
  
  const halfWidth = signatureWidth / 2;
  const halfHeight = signatureHeight / 2;
  
  const x1 = centerX - halfWidth; // esquina inferior izquierda X
  const y1 = centerY - halfHeight; // esquina inferior izquierda Y
  const x2 = centerX + halfWidth; // esquina superior derecha X
  const y2 = centerY + halfHeight; // esquina superior derecha Y
  
  return { x1, y1, x2, y2 };
};

/**
 * Formatear coordenadas para mostrar en la UI
 * @param posX - Posición X
 * @param posY - Posición Y
 * @returns String formateado de coordenadas
 */
export const formatCoordinates = (posX: number, posY: number): string => {
  return `(${posX}, ${posY})`;
};

/**
 * Obtener información completa de conversión para debugging
 * @param page - Página en formato backend
 * @param posX - Posición X
 * @param posY - Posición Y
 * @returns Objeto con información de conversión
 */
export const getConversionInfo = (page: number, posX: number, posY: number) => {
  const frontendPage = convertToFrontendPage(page);
  return {
    frontendPage,
    backendPage: page,
    coordinates: formatCoordinates(posX, posY),
    info: `Backend: Página ${page} | Frontend: Página ${frontendPage}`,
    pyhankoFormat: `PyHanko recibirá: página=${page}, posX=${posX}, posY=${posY}`
  };
};

/**
 * Validar que las coordenadas estén en el rango válido para PyHanko
 * @param page - Página
 * @param posX - Posición X
 * @param posY - Posición Y
 * @returns true si las coordenadas son válidas
 */
export const validatePyHankoCoordinates = (page: number, posX: number, posY: number): boolean => {
  return page >= 0 && posX >= 0 && posY >= 0;
};

/**
 * Crear payload completo para PyHanko con todas las conversiones
 * @param originalData - Datos originales del frontend
 * @returns Payload convertido para PyHanko
 */
export const createPyHankoPayload = (originalData: {
  page: number;
  posX: number;
  posY: number;
  userId: number;
  [key: string]: any;
}) => {
  const backendPage = convertToBackendPage(originalData.page);
  const signatureBox = calculateSignatureBox(originalData.posX, originalData.posY);
  
  return {
    ...originalData,
    page: backendPage,
    posX: signatureBox.x1,
    posY: signatureBox.y1,
    // Agregar información de conversión para debugging
    _conversion: {
      original: {
        page: originalData.page,
        posX: originalData.posX,
        posY: originalData.posY
      },
      converted: {
        page: backendPage,
        posX: signatureBox.x1,
        posY: signatureBox.y1,
        signatureBox
      }
    }
  };
};
