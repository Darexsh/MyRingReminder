# Política de privacidad – MyRingReminder

Last updated: 2026-11-05

## 1. Responsable del tratamiento
Daniel Sichler (Darexsh)  
Correo electrónico: sichler.daniel@gmail.com

## 2. Ámbito de aplicación
Esta política se aplica a la aplicación Android MyRingReminder (`com.darexsh.myringreminder`), distribuida a través de este repositorio y mediante versiones APK.

## 3. Datos que trata MyRingReminder
MyRingReminder trata los datos necesarios para el seguimiento del ciclo y la personalización, entre ellos:
- fechas y antecedentes del ciclo
- registros del periodo (por ejemplo, la indicación de un día de periodo, la intensidad del sangrado, síntomas como dolor o enfermedad y marcas opcionales de inicio y fin)
- ajustes de recordatorios y horarios de notificación
- notas introducidas por la persona usuaria
- preferencias de diseño (colores, fondo e idioma)
- ajustes opcionales del bloqueo de la aplicación

## 4. Almacenamiento local de datos
MyRingReminder almacena tus datos localmente en tu dispositivo (por ejemplo, mediante SharedPreferences o el almacenamiento local de la aplicación).

MyRingReminder no sube al desarrollador tus datos personales del ciclo ni tus notas.

Si utilizas la copia de seguridad o la restauración, los datos se exportan o importan como un archivo local que seleccionas mediante el selector de archivos de Android.

Si exportas los detalles del periodo a PDF, el PDF se genera localmente en tu dispositivo y solo se guarda en la ubicación que eliges en el selector de archivos de Android.  
Al abrir ese PDF desde una notificación de la aplicación, Android puede transferir el archivo a la aplicación de visualización de PDF que elijas. En ese caso, dicha aplicación procesa el archivo conforme a su propia política de privacidad.

## 5. Permisos utilizados
MyRingReminder puede solicitar los permisos necesarios para sus funciones, como:
- notificaciones y alarmas exactas (recordatorios del ciclo)
- acceso a archivos o contenido multimedia (selección de una imagen de fondo y copias de seguridad/restauración)
- datos biométricos o credenciales del dispositivo (bloqueo opcional de la aplicación)

Los permisos se utilizan únicamente para ofrecer las funciones solicitadas de la aplicación.

## 6. Analítica, seguimiento e informes de fallos
Según la implementación actual, MyRingReminder **no** incluye SDK de terceros para analítica ni para informes de fallos.

## 7. Red y enlaces externos
Las funciones principales de MyRingReminder son locales y no requieren una API propia de backend para el seguimiento del ciclo.

Para comprobar si hay actualizaciones, MyRingReminder puede solicitar metadatos de versiones a la API pública de GitHub Releases de este proyecto.

Al abrir **Ajustes → Prospecto del medicamento**, la aplicación solicita el archivo público `leaflet-packages.json` del repositorio GitHub de este proyecto. Al abrir **Información de la aplicación → Política de privacidad**, solicita el archivo de la política correspondiente al idioma seleccionado de la aplicación; si no hay una traducción disponible, se utiliza la versión en inglés. Estas solicitudes no incluyen tus datos del ciclo, notas ni ajustes de la aplicación. GitHub puede tratar información habitual de conexión, como tu dirección IP y los encabezados de la solicitud, conforme a su política de privacidad. La aplicación conserva en tu dispositivo la última copia descargada correctamente de cada idioma, para que puedas leerla si una solicitud posterior falla.

Cuando tú lo solicitas, MyRingReminder puede abrir destinos externos mediante intents del sistema, entre ellos:
- correo electrónico (`mailto:`)
- Linktree
- Telegram
- GitHub
- Buy Me a Coffee
- prospectos alojados por servicios externos de información sobre medicamentos o fabricantes

Al abrir un prospecto, Android pasa su URL al navegador o visor de documentos que elijas. El servicio que aloja el documento recibe la solicitud; la URL puede revelar el producto y el idioma del prospecto. El navegador, el visor y el proveedor del prospecto pueden tratar información habitual de conexión conforme a sus propias políticas de privacidad. MyRingReminder no adjunta a estas solicitudes los datos del ciclo, las notas ni los ajustes almacenados localmente.

Al abrir enlaces externos, se aplican las políticas de privacidad de esos servicios de terceros.

### Servicios de terceros mencionados en los enlaces
- GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement
- Telegram: https://telegram.org/privacy
- Linktree: https://linktr.ee/s/privacy
- Buy Me a Coffee: https://www.buymeacoffee.com/privacy-policy

## 8. Comunicación de datos
Según la implementación actual, MyRingReminder no transmite al desarrollador los datos del ciclo, las notas ni los ajustes almacenados localmente.

## 9. Aviso médico
MyRingReminder es únicamente una herramienta de organización y seguimiento.

No es un producto sanitario, no ofrece diagnósticos médicos y no sustituye el asesoramiento de profesionales de la salud.

Si tienes preguntas médicas o problemas de salud, consulta con un profesional sanitario cualificado.

## 10. Tus opciones
- Puedes editar o eliminar tus datos dentro de la aplicación.
- Puedes revocar los permisos de la aplicación en los ajustes de Android.
- Puedes optar por no abrir enlaces externos.
- Puedes desinstalar la aplicación y borrar los datos locales desde los ajustes de Android.

## 11. Cambios en esta política
Esta política puede actualizarse si cambian las funciones de la aplicación o los requisitos legales.
