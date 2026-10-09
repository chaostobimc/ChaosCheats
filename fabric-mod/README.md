# CHAOS Client – Fabric-Mod (Minecraft 1.21.11)

Reines Client-GUI im Stil eines Hack-Clients. **Es hat keinerlei Funktion**: Module, Slider,
Keybinds und Configs ändern nur die Anzeige. Es werden keine Spielwerte, Pakete oder
Eingaben manipuliert.

## Inhalt
- Taste **Rechts-Shift** öffnet das Menü (in den Steuerungseinstellungen änderbar).
- Kategorien mit ~110 Fake-Modulen, Toggles, Keybinds (Klick auf die Taste, dann neue Taste drücken).
- Detailpanel mit Slidern und Checkboxen, Konfigurationsliste, Settings (Rainbow-Akzent, Konsole usw.).
- Boot-Intro beim ersten Öffnen pro Spielstart.

## Voraussetzungen
- Minecraft **1.21.11**
- Fabric Loader ab 0.19.5, Fabric API 0.141.6+1.21.11
- Java 21

## Bauen
```bash
./gradlew build
```
Das fertige JAR liegt danach in `build/libs/`. In den Ordner `mods/` kopieren.

Zum Testen im Dev-Client: `./gradlew runClient`.

## Hinweise
- GUI-Skalierung auf „Normal“ oder „Klein“ stellen, sonst ist das Fenster sehr eng.
- Der Mod-Name steht in `src/main/resources/fabric.mod.json`, dort lässt sich alles anpassen.
