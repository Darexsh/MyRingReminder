# Datenschutzerklärung – MyRingReminder

Last updated: 2026-11-05

## 1. Verantwortlicher
Daniel Sichler (Darexsh)  
E-Mail: sichler.daniel@gmail.com

## 2. Geltungsbereich
Diese Datenschutzerklärung gilt für die Android-App MyRingReminder (`com.darexsh.myringreminder`), die über dieses Repository und als APK-Versionen verteilt wird.

## 3. Welche Daten MyRingReminder verarbeitet
MyRingReminder verarbeitet App-Daten, die für die Zyklusverfolgung und Personalisierung erforderlich sind. Dazu gehören:
- Zyklusdaten und Zyklusverlauf
- Einträge zur Periodenverfolgung (zum Beispiel Kennzeichnung eines Periodentages, Blutungsstärke, Symptomkennzeichnungen wie Schmerzen oder Krankheit sowie optionale Start- und Endmarkierungen)
- Erinnerungseinstellungen und Benachrichtigungszeiten
- von dir eingegebene Notizen
- Designeinstellungen (Farben, Hintergrund, Sprache)
- optionale Einstellungen für die App-Sperre

## 4. Lokale Datenspeicherung
MyRingReminder speichert deine Daten lokal auf deinem Gerät (zum Beispiel in SharedPreferences oder im lokalen App-Speicher).

MyRingReminder lädt deine persönlichen Zyklusdaten oder Notizen nicht zum Entwickler hoch.

Wenn du die Sicherung oder Wiederherstellung verwendest, werden Sicherungsdaten als lokale Datei exportiert beziehungsweise importiert, die du über die Android-Dateiauswahl auswählst.

Beim PDF-Export der Periodendetails wird die PDF-Datei lokal auf deinem Gerät erstellt und nur an dem Speicherort abgelegt, den du in der Android-Dateiauswahl bestimmst.  
Wenn du diese PDF über die App-Benachrichtigung öffnest, kann Android die Datei an eine von dir ausgewählte PDF-Viewer-App übergeben. In diesem Fall verarbeitet diese Viewer-App die Datei gemäß ihrer eigenen Datenschutzerklärung.

## 5. Verwendete Berechtigungen
MyRingReminder kann Berechtigungen anfordern, die für App-Funktionen benötigt werden, zum Beispiel:
- Benachrichtigungen und genaue Alarme (Zykluserinnerungen)
- Medien- und Dateizugriff (Hintergrundbild auswählen, Sicherung und Wiederherstellung)
- biometrische Daten oder Gerätezugangsdaten (optionale App-Sperre)

Berechtigungen werden ausschließlich für die angeforderten App-Funktionen verwendet.

## 6. Analyse, Tracking und Absturzberichte
Nach aktuellem Stand enthält MyRingReminder **keine** SDKs von Drittanbietern für Analyse oder Absturzberichte.

## 7. Netzwerkverbindungen und externe Links
Die Kernfunktionen von MyRingReminder sind lokal ausgerichtet. Für die Zyklusverfolgung ist keine eigene Backend-API erforderlich.

Für die Suche nach Updates kann MyRingReminder Release-Metadaten über die öffentliche GitHub-Releases-API dieses Projekts abrufen.

Wenn du **Einstellungen → Packungsbeilage** öffnest, ruft die App die öffentliche Datei `leaflet-packages.json` aus dem GitHub-Repository dieses Projekts ab. Wenn du **App-Info → Datenschutzerklärung** öffnest, ruft sie die zur ausgewählten App-Sprache gehörende Datei aus demselben Repository ab; falls keine Übersetzung verfügbar ist, wird Englisch verwendet. Bei diesen Anfragen werden deine Zyklusdaten, Notizen oder App-Einstellungen nicht mitgesendet. GitHub kann gewöhnliche Verbindungsinformationen wie deine IP-Adresse und Request-Header gemäß seiner Datenschutzerklärung verarbeiten. Die App speichert die zuletzt erfolgreich abgerufene Version jeder Sprache auf deinem Gerät, damit du sie auch lesen kannst, falls eine spätere Anfrage fehlschlägt.

Auf deine Veranlassung kann MyRingReminder über System-Intents externe Ziele öffnen, darunter:
- E-Mail (`mailto:`)
- Linktree
- Telegram
- GitHub
- Buy Me a Coffee
- Packungsbeilagen von externen Arzneimittelinformationsdiensten oder Herstellern

Wenn du eine Packungsbeilage öffnest, übergibt Android deren URL an den von dir ausgewählten Browser oder Dokumentenbetrachter. Der ausgewählte Hosting-Dienst erhält die Anfrage; die URL kann das Produkt und die Sprache der Packungsbeilage erkennen lassen. Browser, Viewer und Anbieter der Packungsbeilage können gewöhnliche Verbindungsinformationen gemäß ihren eigenen Datenschutzerklärungen verarbeiten. MyRingReminder fügt diesen Anfragen keine lokal gespeicherten Zyklusdaten, Notizen oder Einstellungen hinzu.

Für externe Links gelten die Datenschutzerklärungen der jeweiligen Drittanbieter.

### In Links erwähnte Drittanbieter
- GitHub: https://docs.github.com/en/site-policy/privacy-policies/github-privacy-statement
- Telegram: https://telegram.org/privacy
- Linktree: https://linktr.ee/s/privacy
- Buy Me a Coffee: https://www.buymeacoffee.com/privacy-policy

## 8. Weitergabe von Daten
Nach aktuellem Stand der App übermittelt MyRingReminder deine lokal gespeicherten Zyklusdaten, Notizen oder Einstellungen nicht an den Entwickler.

## 9. Medizinischer Hinweis
MyRingReminder dient ausschließlich der Organisation und Dokumentation.

Die App ist kein Medizinprodukt, stellt keine medizinischen Diagnosen und ersetzt keine professionelle medizinische Beratung.

Wende dich bei medizinischen Fragen oder gesundheitlichen Beschwerden an qualifiziertes medizinisches Fachpersonal.

## 10. Deine Möglichkeiten
- Du kannst deine Daten in der App bearbeiten oder löschen.
- Du kannst App-Berechtigungen in den Android-Einstellungen widerrufen.
- Du kannst darauf verzichten, externe Links zu öffnen.
- Du kannst die App deinstallieren und lokale Daten in den Android-Einstellungen löschen.

## 11. Änderungen dieser Datenschutzerklärung
Diese Datenschutzerklärung kann aktualisiert werden, wenn sich App-Funktionen oder rechtliche Anforderungen ändern.
