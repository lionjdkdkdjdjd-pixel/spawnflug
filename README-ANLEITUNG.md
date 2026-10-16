# Spawnflug – Minecraft Fabric 26.3

**Wichtig:** Dies ist ein ungetestetes Java-/Gradle-Quellprojekt, keine fertige Mod-JAR.
Zum Bauen werden Java 25, Gradle 9.6.1 sowie Internetzugang fuer die
Minecraft-/Fabric-Abhaengigkeiten benoetigt.

## Funktion
- Im Survival am Spawn abspringen, in der Luft noch einmal Leertaste druecken.
- Der Server aktiviert Elytra-Gleiten ohne ausgeruestete Elytra.
- Leertaste waehrend des Fluges gibt einmal einen Schub.
- Landen oder Wasser beendet das Gleiten.
- Kein Client-Mod erforderlich; nur Server.
- Standardradius: 30 Bloecke, Boost: 1.6.

## GitHub: fertig bauen lassen (ohne Java auf deinem PC)
1. Konto auf https://github.com/ erstellen (falls noch nicht vorhanden).
2. Neues Repository erstellen, z.B. `spawnflug` (privat geht auch).
3. Das ZIP entpacken. Bei GitHub im Repository "Add file" -> "Upload files".
   Die INHALTE des entpackten Ordners inklusive Unterordner hochladen.
   Der Ordner `.github/workflows` muss im Repository erhalten bleiben.
   GitHub erlaubt Drag & Drop von Ordnern, aber manchmal blockiert der Browser
   Dateien, die mit Punkt beginnen. Gegebenenfalls die Workflowdatei unter
   `.github/workflows/build.yml` direkt im GitHub-Webeditor erstellen und
   ihren Inhalt aus dieser ZIP kopieren.
4. Auf "Commit changes" klicken.
5. Reiter "Actions" -> Workflow "Spawnflug 26.3 bauen" -> "Run workflow".
6. Nach einem erfolgreichen gruenen Lauf die Datei/den Bereich "Artifacts"
   -> "spawnflug-fabric-26.3" herunterladen und die ZIP entpacken.
7. Die Datei `spawnflug-fabric-26.3-0.1.0.jar` unter GPORTAL in den Ordner
   `mods` hochladen. NICHT die Quellcode-ZIP hochladen.
8. Fabric API 26.3 muss installiert sein. Server starten.

## Konsole / Minecraft-Befehle
Im Spiel als OP mit Slash, in der Serverkonsole ohne Slash:
- `/spawnflug info`
- `/spawnflug center` (auf dem Spawn stehen!)
- `/spawnflug radius 30`
- `/spawnflug boost 1.6`
- `/spawnflug reset`

Die Einstellungen werden in `config/spawnflug.json` gespeichert.

## Pruefen
1. Spieler ohne Elytra, Survival-Modus, am Spawn.
2. Springen und in der Luft nochmals Space druecken.
3. Einmal Space zum Boost; Landen beendet die Flugfunktion.
4. Weiter weg vom Spawn noch einmal versuchen: dort darf kein neuer Flug starten.

## Bekannte Einschraenkungen
- Der Server kann Spieler zum Gleiten zwingen, aber das Verhalten unmodifizierter
  Clients beim Abheben muss auf einem echten Fabric-26.3-Server getestet werden.
- Java-API-Details koennen zu einem Kompilierungsfehler fuehren. Schicke in dem
  Fall den GitHub-Actions-Log; dann kann der Quellcode korrigiert werden.
- Der Gleitflug selbst erzeugt serverseitig kein Elytra-Item; eine zugehoerige
  Animation der Spielerpose kann clientseitig sichtbar sein.
- Es wird kein Fallschaden-Schutz fuer sonstige Spruenge vergeben.
