package de.chaos.fakeclient.gui;

import java.util.ArrayList;
import java.util.List;

/** Die Modul-Liste. Format: kategorie|Name|Beschreibung */
public final class ModuleCatalog {

	public static final String[] TAB_IDS = {
		"combat", "movement", "visuals", "player", "world", "exploits", "render", "scripts", "misc",
		"configs", "settings"
	};
	public static final String[] TAB_LABELS = {
		"Combat", "Movement", "Visuals", "Player", "World", "Exploits", "Render", "Scripts", "Misc",
		"Configs", "Settings"
	};
	/** Die ersten 9 Tabs sind Modul-Kategorien. */
	public static final int MODULE_TABS = 9;

	private static final String[] RAW = {
		"combat|AimAssist Pro|Glättet deine Mausbewegung Richtung Ziel",
		"combat|Triggerbot|Löst aus, sobald das Ziel im Fadenkreuz liegt",
		"combat|Reach+|Erhöht die Trefferreichweite um einige Blöcke",
		"combat|KillAura Lite|Rotiert automatisch zu Gegnern in der Nähe",
		"combat|Velocity|Reduziert Rückstoß und Knockback spürbar",
		"combat|AutoBlock|Blockt eingehende Treffer im richtigen Moment",
		"combat|Backtrack|Verarbeitet Gegnerpositionen aus der Vergangenheit",
		"combat|HitBox Expand|Vergrößert die Trefferfläche der Gegner",
		"combat|Criticals|Erzwingt kritische Treffer beim Angriff",
		"combat|Sword Combo|Verkettet Schwertangriffe ohne Verzögerung",
		"combat|TargetStrafe|Umkreist das Ziel mit adaptiver Geschwindigkeit",
		"combat|W-Tap Sprint|Setzt Sprint-Resets für mehr Schaden",
		"combat|Antibot Filter|Ignoriert NPCs und Bot-Entitäten",

		"movement|Speed Strafe|Beschleunigt die Bewegung in der Luft",
		"movement|Fly Glide|Gleitflug mit einstellbarer Höhe",
		"movement|NoFall|Verhindert Fallschaden bei der Landung",
		"movement|High Jump|Erhöht die Sprunghöhe",
		"movement|Step Assist|Steigt Blöcke ohne Sprung hinauf",
		"movement|Spider Walk|Klettert an Wänden empor",
		"movement|Sprint Plus|Hält den Sprint dauerhaft aktiv",
		"movement|Jesus Walk|Läuft über Wasser und Lava",
		"movement|Blink Dash|Kurzer Sprung in Blickrichtung",
		"movement|Phase Lite|Bewegt dich durch dünne Wände",
		"movement|AutoWalk|Läuft automatisch geradeaus weiter",
		"movement|Air Stuck|Friert deine Position in der Luft ein",
		"movement|Long Jump|Maximiert die Weite deines Sprungs",

		"visuals|ESP Boxes|Zeichnet Boxen um Spieler",
		"visuals|Chams Glow|Färbt Entitäten durch Wände",
		"visuals|Nametags+|Zeigt Namen, HP und Ausrüstung",
		"visuals|Tracers|Linien zu allen Zielen im Sichtfeld",
		"visuals|Fullbright|Macht die Welt jederzeit hell",
		"visuals|X-Ray Ores|Zeigt Erze durch den Untergrund",
		"visuals|Storage ESP|Markiert Truhen und Fässer",
		"visuals|Trajectories|Zeigt Flugbahnen von Projektilen",
		"visuals|Custom Crosshair|Eigenes Fadenkreuz mit Farbanpassung",
		"visuals|Zoom Scope|Stufenloser Zoom per Mausrad",
		"visuals|Motion Blur Off|Entfernt Bewegungsunschärfe",
		"visuals|Particle Remover|Blendet Partikeleffekte aus",
		"visuals|Freelook|Kamera löst sich vom Körper",

		"player|AutoArmor|Rüstet automatisch die beste Rüstung an",
		"player|InvCleaner|Sortiert und räumt dein Inventar auf",
		"player|ChestStealer|Leert Truhen in Sekundenbruchteilen",
		"player|AutoTool|Wechselt automatisch das passende Werkzeug",
		"player|FastPlace|Entfernt Verzögerung beim Platzieren",
		"player|NoSlow|Keine Geschwindigkeitsreduktion beim Essen",
		"player|Inventory Move|Bewegen im offenen Inventar",
		"player|Auto Respawn|Respawnt sofort nach dem Tod",
		"player|Nuker Lite|Baut Blöcke im Radius ab",
		"player|Scaffold|Baut Brücken unter deine Füße",
		"player|Timer|Verändert die Spiel-Tickrate",
		"player|AutoClicker|Klickt, bis du es wieder ausschaltest",
		"player|Hotbar Sorter|Ordnet die Schnellleiste nach Typ",

		"world|Nuker|Entfernt Blöcke im Umkreis automatisch",
		"world|Fast Break|Reduziert die Abbauzeit drastisch",
		"world|Tree Chopper|Fällt ganze Bäume mit einem Schlag",
		"world|AutoFarm|Erntet reife Pflanzen automatisch",
		"world|Cave Finder|Markiert Höhlen auf der Karte",
		"world|Ore Scanner|Scannt Chunks nach Erzvorkommen",
		"world|Lava Bridge|Platziert Blöcke über Lava",
		"world|Auto Fish|Angelt und sammelt automatisch",
		"world|Auto Mine|Baut einen Schacht in Abwärtsrichtung",
		"world|Bed Finder|Zeigt Betten von Gegnern an",
		"world|Chunk Ghost|Lädt Chunks ohne Kollision",
		"world|Weather Control|Ändert Wetter und Tageszeit",

		"exploits|Packet Spoof|Manipuliert ausgehende Pakete",
		"exploits|Desync|Trennt Client- und Server-Position",
		"exploits|Ghost Hand|Interagiert aus der Distanz",
		"exploits|Lag Switch|Friert die Verbindung kurzzeitig ein",
		"exploits|Fake Lag|Erzeugt künstliche Latenz",
		"exploits|Bypass Rev7|Umgeht Standard-Prüfungen",
		"exploits|Anticheat Bypass v3|Überlistet die Erkennung",
		"exploits|Chat Bypass|Umgeht Chatfilter mit Unicode",
		"exploits|Crash Proof|Verhindert Abstürze bei Fehlern",
		"exploits|Ban Shield|Verschleiert deine Client-Signatur",
		"exploits|Silent Teleport|Teleportiert ohne Bewegungsanzeige",
		"exploits|Mod Scanner Off|Deaktiviert Modul-Scans",
		"exploits|Packet Logger|Protokolliert alle Netzwerkpakete",

		"render|Shader Core|Lädt den Shader-Stack neu",
		"render|Bloom|Weiches Leuchten für helle Flächen",
		"render|Vignette|Dunkle Ränder für mehr Atmosphäre",
		"render|Night Vision|Macht dunkle Bereiche sichtbar",
		"render|Blur HUD|Glasmorphismus für HUD-Elemente",
		"render|Watermark|Zeigt Client-Logo und FPS",
		"render|ArrayList|Aktive Module als Liste anzeigen",
		"render|Keystrokes|Zeigt gedrückte Tasten an",
		"render|Armor HUD|Zeigt den Rüstungsstatus an",
		"render|Coordinates|Blendet Koordinaten ein",
		"render|Potion HUD|Listet aktive Effekte auf",
		"render|Target HUD|Infos zum anvisierten Gegner",
		"render|Fog Remover|Entfernt Nebel komplett",

		"scripts|Script Loader|Lädt externe Skripte im Sandbox-Modus",
		"scripts|Lua Runner|Führt Lua-Skripte im Hintergrund aus",
		"scripts|JS Macros|Eigene Makros in JavaScript",
		"scripts|Macro Recorder|Zeichnet Eingaben als Makro auf",
		"scripts|Chat Macros|Schnelle Nachrichten per Tastendruck",
		"scripts|Script Store|Durchsuche die Community-Skripte",
		"scripts|Auto Update|Aktualisiert Skripte automatisch",
		"scripts|Auto Macros|Startet Makros nach Zeitplan",
		"scripts|Macro Sharing|Teile Makros mit deinen Freunden",

		"misc|Discord RPC|Zeigt deinen Status auf Discord",
		"misc|Auto Reconnect|Verbindet bei Abbruch neu",
		"misc|Auto GG|Schreibt nach jedem Sieg eine Nachricht",
		"misc|Name Protect|Verbirgt deinen Namen im Chat",
		"misc|Anti AFK|Verhindert Kick durch Inaktivität",
		"misc|Chat Filter|Blendet unerwünschte Nachrichten aus",
		"misc|Auto Login|Speichert deine Anmeldung sicher",
		"misc|Sound Mute|Stummschaltung bei bestimmten Ereignissen",
		"misc|Screenshot Plus|Automatische Screenshots bei Kills",
		"misc|Clock Overlay|Zeigt Uhrzeit und Session-Dauer"
	};

	private ModuleCatalog() {
	}

	public static List<FakeModule> create() {
		List<FakeModule> list = new ArrayList<>();
		for (String line : RAW) {
			String[] parts = line.split("\\|");
			list.add(new FakeModule(parts[0], parts[1], parts[2]));
		}
		return list;
	}
}
