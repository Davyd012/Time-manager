# Hjem ↔ Kalender – endelig UX-/UI- og implementeringsplan

**Prosjekt:** Tidsstyring (Kotlin Multiplatform, Compose Multiplatform)  
**Referanser:** Opprinnelig opptak `18379.mp4` (8,69 s) og nytt opptak `Screen_Recording_20261111_141124_Tidsstyring.mp4` (6,53 s).  
**Status:** Design- og implementeringsspesifikasjon. Filer, kode og ytelse i eksisterende prosjekt er ikke undersøkt; årsaksforklaringer er hypoteser som må verifiseres i kildekoden.

## 1. Mål

Lag en sammenhengende, avbrytbar, fingerstyrt overgang mellom Hjem (ukestripe/dagsinnhold) og Kalender (måned/detaljer). Det skal føles som én interaktiv overflate – ikke to separate skjermer eller en tradisjonell navigasjonsovergang. Ta utgangspunkt i den nyere videoens mulighet til å dra fra hovedområdet, men forbedre samspill mellom header, ukestripe, månedskort, dagsinnhold, bunnnavigasjon og enter-animasjon.

Behold eksisterende mørk visuell stil, sand-/kremfarget aksent, avrundede kort, typografi, ikoner og registreringsfunksjoner. Målet er ikke en total redesign.

## 2. Observasjoner fra opptakene

### Opprinnelig opptak
- Draområdet virket begrenset til header/håndtak; ønsket er å kunne dra fra hovedområdet.
- Ved åpning/lukking er Hjem- og Kalender-elementer synlige samtidig. Tekst og ukestripe overlapper.
- Visuell bevegelse er hakkete/urytmisk, særlig rundt overgang og avslutning.
- Etter lukking animeres illustrasjon/tekst/knapp inn på nytt og det oppleves som dobbelt enter-animasjon.

### Nytt opptak
- Rundt 1,4–3,2 s: kalenderen åpnes via bevegelse fra innholdsområdet; dette gir bedre opplevelse av kontroll.
- Kalenderen beveger seg opp innenfor visningen, men det er mye mørklegging og gjennomskinn av Hjem mens kalenderen kommer frem.
- Rundt 3,1–3,3 s er det et tydelig skifte fra delvis fremdratt kalender til fullt navigert/utvidet kalendervisning.
- Rundt 4,7–5,1 s lukkes kalenderen, og tomtilstanden på Hjem blir synlig igjen; denne overgangen bør være helt kontinuerlig uten ekstra re-entry.
- Ulike forkortelser for ukedager (engelsk i ukestripe, norsk i månedskalender).
- Månedskalenderen har små datotall, små registreringssummer og betydelig ubrukt rom nedenfor.

*Merk:* Videoer viser visuell oppførsel, ikke underliggende Kotlin-kode. Vi kan ikke slå fast at skjermene faktisk reinitialiseres uten å inspisere komposisjon, nav-backstack, state og effektkall.

## 3. Endelig visuell opplevelse

### A. Hjem – lukket (progress = 0)
1. Statuslinje respekterer edge-to-edge/insets som i dag.
2. Stabil appheader: valgt ukedag (f.eks. Onsdag), innstillinger/handlinger.
3. Ukestripe med 7 dager, tydelig valgt dag og konsekvent norsk lokalisering.
4. Subtilt drahåndtak under ukestripen; hele egnet hovedareal er også gestflate.
5. Dagens innhold: behold eksisterende illustrasjon, «Ingen arbeidsføringer», hjelpetekst og «Legg til timer».
6. Bunnnavigasjon er stabil og påvirkes ikke av kalenderbevegelsen.

### B. Mellomposisjon (0 < progress < 1)
1. Én fysisk overflate stiger opp og dekker Hjem, med heldekkende bakgrunn som hindrer uønsket gjennomsyn.
2. Kalenderoverflaten og eventuell headerendring styres av én og samme `progress`.
3. Innhold som må vises i begge tilstander skal enten gjenbrukes som delt element eller ha eksplisitte, ikke-overlappende visningsgrenser.
4. Månedskort, månedsnavigasjon og oppsummering beveger seg som én gruppe – ikke med hver sin forsinkelse.
5. Ingen ekstra fade-in for tomtilstanden mens overflaten beveger seg.
6. Ingen selvstendige navigation-enter/-exit-animasjoner samtidig med drag.

### C. Kalender – åpen (progress = 1)
1. Header «Kalender», tydelig tilbakehandling, relevante kontroller for filter/dato.
2. Månedsvelger med forrige/neste og måned+år, uten at trykk starter lukke-gest.
3. Månedskalender med gode trykkflater (mål ca. 44–48 dp), lesbarhet, valgt dato, dagens dato og indikatorer for registrerte timer.
4. Månedstotal med lesbar tekst («39 t 30 min», basert på eksempel i opptaket), ikke en mikroskopisk etikett.
5. Under kalenderen: «Valgt dag» med faktiske arbeidsføringer eller tomtilstand. Bruk rommet konstruktivt, men ikke press inn unødvendig innhold.
6. Samme vedvarende bunnnavigasjon som på Hjem.

## 4. Gestekontrakt

| Start/handling | Forventet resultat |
|---|---|
| Dra opp fra tomt hovedområde på Hjem | Åpne Kalender, kontinuerlig med fingeren |
| Dra opp fra håndtak | Åpne Kalender |
| Dra ned fra kalenderens header, kort eller tomrom | Lukk Kalender |
| Dra ned fra grid med scrollbart detaljinnhold | Scroll innhold først; lukk bare hvis innholdet er øverst og gesten fortsetter ned |
| Trykk på dato | Velg dag, ikke lukk/åpne |
| Horisontalt sveip i ukestripe | Behold eksisterende dag-/ukebetjening |
| Trykk på knapper | Normal klikkadferd; ikke kapre input |
| Sakte drag, slippe før terskel | Returner rolig til startanker |
| Raskt fling | Fullfør til anker i retningen hvis hastighetsterskelen nås |
| Ny finger under settle-animasjon | Avbryt gammel animasjon og gi kontroll til fingeren |
| Systemtilbake når Kalender er åpen | Lukk Kalender først; ikke forlat Hjem-route |
| Tilgjengelighet, redusert animasjon | Tilby eksplisitt knapp og kort/ingen bevegelse |

**Forslag til innstillinger:** touch slop fra plattformen, terskel 30–40 % av avstanden mellom ankerne (test med brukere), flinggrense ca. 800–1200 dp/s (justeres), settle typisk 240–360 ms ved normal avstand; kritisk dempet/lett dempet fjær uten tydelig overshoot. Tallene er designstartverdier, ikke absolutte ytelseskrav.

## 5. Felles tilstand og arkitektur

- Behold samme `HomeRoute` og `HomeViewModel` mens kalenderen åpnes; `CalendarExpanded` er som hovedregel en UI-tilstand, ikke en separat rute i backstacken. Om appen trenger deep link/egen rute for kalender, adapter denne til samme overflatestruktur uten å kjøre to overgangssystemer.
- Én tilstandsholder (`HomeCalendarTransitionState`) eier `Collapsed`/`Expanded`-ankere, offset, progress og pågående settle.
- Domene-/UI-state (valgt dato, måned, arbeidsføringer, timer og loading) lagres separat fra overgangstilstanden. Ikke nullstill data ved lukking.
- Bruk `AnchoredDraggableState`/`Modifier.anchoredDraggable` i `commonMain` når API-versjonen i prosjektet støtter dette, med `DraggableAnchors` beregnet fra målt tilgjengelig areal og trygge inset. Sjekk nøyaktig signatur for installert Compose-versjon; API har endret seg.
- Unngå å registrere to uavhengige dragdetektorer på overlappende foreldre/barn.
- Behold Home og Calendar i samme `Box`/shell for jevn compositing, men bare én synlig kalenderoverflate og ett logisk header-eierskap. Lag gjerne `HomeCalendarHost` med separate `HomeContent`, `CalendarContent`, `SharedTopBar`, `BottomNavigation`.
- Gestebare områder kan markeres eksplisitt; koordinering med vertikal scroll gjøres via `nestedScroll`.
- Klikk og drag deles med berøringsslop og retning før drag eier gesten. Bruk semantiske handlinger for tastatur/skjermleser.

## 6. Animasjonskontrakt

**Én progress-verdi fra 0 til 1** bestemmer panelposisjon, clipping og eventuelle dekorative endringer. Ikke koble animasjon til en boolean `isCalendarVisible` flere steder med uavhengige `AnimatedVisibility`, `AnimatedContent` eller `LaunchedEffect`.

**Under drag:** ingen tween per pointer delta, ingen ekstra alfa-animasjon som blir liggende etter fingeren; oppdater offset direkte.

**Når gesten slippes:** ett settle-kall til nærmeste/mål-anker, avbrytbart ved ny gest. Bruk en behersket fjær. Ikke start en ny navigation transition når kalenderen når fullt åpen.

**Visuell blending:** prioriter opaque panel/clip fremfor en kraftig scrim som viser tekst fra begge skjermer. Om scrim brukes, hold den subtil og synkronisert. Hold daginnholdet stående bak kalenderpanelet i stedet for å remontere det.

**Header:** kan crossfade raskt i et kontrollert intervall, men ikke ha to overskrifter synlige oppå hverandre. Et mer avansert alternativ er en delt header med ett layoutsystem og interpolate posisjon, størrelse og tilknyttede handlinger.

**Bottom bar:** konstant posisjon gjennom hele overgangen, ingen ekstra slide/fade/recomposition-basert re-entry.

## 7. Fjern dobbelt enter/reload-effekt

Undersøk og logg følgende, før retting:

1. Om `HomeScreen` forlater komposisjonen ved åpning og bygges på nytt ved lukking.
2. Om `AnimatedVisibility`/`AnimatedContent` trigger enter ved hver `visible`-endring eller `contentKey`-endring.
3. Om `LaunchedEffect(Unit)` starter introanimasjon når komponenten remonteres.
4. Om navigasjon, route key eller `remember`/`rememberSaveable` resetter skjermtilstand.
5. Om en flakkende `Loading -> Empty -> Loading -> Empty` medfører re-entry.
6. Om illustrasjon/tekst/CTA hver har en egen introanimasjon som forsinkes uavhengig.

**Korrigering:** Bevar Home-komposisjonen og tilstanden. Gating av en valgfri første-gangs-intro må skje en gang per ønsket levetid (f.eks. per appstart), ikke hver gang kalenderen lukkes. Data må ikke refetches uten faktisk grunn.

## 8. Ytelse og teknikk

- Bruk lambda-variant av `Modifier.offset { IntOffset(...) }` eller `graphicsLayer { translationY = ... }` for hyppige animasjonsoppdateringer slik at store composable-trær ikke recomponeres per frame.
- Obs: `graphicsLayer` flytter bare tegningen, ikke layout-/hit-test-/semantikkrammene. Valg av offset/lag må samsvare med ønsket klikkgeometri og klipping.
- Les ikke transition-state i toppen av hele `HomeRoute` hvis verdien bare trengs av panel-laget.
- Cache/precompute month-grid fra `YearMonth` og datasett; ikke generer datoliste kontinuerlig under drag.
- Bruk stabile keys for arbeidsføringer og listeelementer; unngå duplisert kalender grid i to trær.
- Test på fysisk Android-enhet og iOS med samme gesture- og state-logikk. Bruk Layout Inspector/profilering/jank-sporing tilgjengelig på plattformen.
- Håndter navigasjonsbar/statuslinje, visning ved ulik oppløsning/tekstskala, rotasjon og IME.

## 9. Slik implementeres det i faser

**Fase 1 – Finn rotårsaken:** identifiser komposisjonsstrukturen, gestemodifikatorene, navigasjonskall, duplicated animasjoner og state-livssyklus. Ta en before-video og legg til midlertidig logging for opprettelser og enter-kall.

**Fase 2 – Stabiliser state:** introduser én delt state holder og to anchore; hold Home montert ved kalenderåpning. Avklar system-back-logikk.

**Fase 3 – Gestemotor:** åpne/lukk fra hovedflate, respekt for trykk og nestet scroll, slop, hastighet og avbrudd. Skriv tester for terskel og fling.

**Fase 4 – Bygg én fysisk overgang:** samme overflate, sammenhengende clipping og koordinert header; bunnnavigasjon stabil. Fjern overlapp og overdrevet dimming.

**Fase 5 – UI-polering:** konsekvent norsk, større berøringsflater, tydelig valgt dato, månedssammendrag og nyttig detaljrom.

**Fase 6 – Ytelse og QA:** profiler, test tilgjengelighet og plattformparitet, spill inn before/after-videoer og dokumenter avvik.

## 10. Definisjon av ferdig

- Drag fra hovedområdet både på Hjem og Kalender fungerer.
- Sakte drag følger fingeren uten synlig etterslep; hurtig fling slutter ved korrekt anker.
- Gest fra dato/knapp gir riktig klikk, ikke utilsiktet navigasjon.
- En ny gest avbryter eksisterende settle umiddelbart.
- Ingen dobbelt header, overlappende ukestripe/måned eller ghosting mellom innholdslag.
- Ingen ny enter-animasjon på Hjem ved lukking.
- Ingen loading-flakk eller datareset ved åpning/lukking.
- Bunnnavigasjon holder seg stabil; alle valg/registreringer beholdes.
- Rotasjon/insets/store fonter håndteres.
- Back, skjermleser og redusert bevegelse fungerer.
- Testet med minst 20 raske åpne/lukkesykluser og et sett langsomme/avbrutte sveip.

## 11. Ferdig prompt for kodeassistent (English)

> Implement a premium, gesture-driven, unified Home ↔ Calendar experience in our Kotlin Multiplatform / Compose Multiplatform time-tracking app. Use the two supplied screen recordings as behavioral references. Preserve the current dark aesthetic, sand accent, existing components, app functionality, and bottom navigation. The second recording improves dragging from the main area, but still has a dim/ghosting crossfade, a visible snap between partially and fully open Calendar, and an unnecessary Home empty-state return animation.
>
> First locate and inspect the actual Home and Calendar composables, state holders, navigation, nested scroll, pointer handling, `AnimatedVisibility`, `AnimatedContent`, `LaunchedEffect`, and transition specs. Report evidence-backed root causes rather than assuming them.
>
> Architect the calendar as an expanded state of one persistent `HomeCalendarHost` whenever feasible. Keep Home composed and its ViewModel/domain state stable. Use exactly one transition controller for collapsed/expanded anchors, offset, progress, and settle. Prefer `AnchoredDraggableState` + `Modifier.anchoredDraggable` in `commonMain` if compatible with our Compose version, with anchors calculated from actual available size and system insets. Do not hardcode screen pixel offsets. Respect touch slop, direction locking, velocity, gesture cancellation, and interruption. Coordinate scrollable children using nested scroll. Let users drag from the main content, handle, and non-control calendar regions; preserve taps on dates, toolbar buttons, actions and horizontal gestures.
>
> During a drag, the panel must track the finger directly and continuously. No per-delta tween, no laggy intermediary Boolean visibility animation. At release, settle exactly once with a subtle, interruptible low-overshoot spring. Render a single opaque/clipped calendar surface that physically moves over Home. Drive visual changes from the same transition progress. Prevent duplicated overlapping headers, weekday strips, calendar grids, or ghosted Home content. Keep bottom navigation fixed and interactive according to current rules. Make the header change coordinated and professional, without two independent navigation animations. Prevent an extra route transition when the gesture reaches the expanded anchor.
>
> Investigate and eliminate the Home empty-state re-entrance on close, including possible remounting, unstable keys, transient loading state, route changes or `LaunchedEffect`-replayed animations. Returning to Home must reveal the existing illustration, copy and CTA at their already-stable position, without fresh fade/slide. Preserve user selections and time entries.
>
> Polish details: Norwegian weekday abbreviations and locale consistency; clear selection of today vs selected date; touch targets approximately 44–48 dp; readable month totals; use the area below the month calendar for selected-day entries or an intentional empty state. Keep the established visual design; don't redesign other screens.
>
> Performance: defer frequently changing offset/alpha reads to layout/draw phase where possible (`Modifier.offset {}` / `graphicsLayer {}`). Avoid full-tree recomposition on each drag frame. Precompute the month grid and use stable keys. Profile real devices and ensure Android/iOS parity. Respect system insets, accessibility, reduced motion and Back/keyboard behavior.
>
> Deliver: (1) root causes with file/line references, (2) concise implementation plan, (3) actual source changes, (4) changed-file list, and (5) verification of each acceptance test: slow drag, fling, release below threshold, interrupting an animation, gestures on dates/buttons, horizontal week navigation, nested scrolling, no visual overlap, no repeated Home entry, fixed bottom nav, Android/iOS when applicable, and repeated 20+ rapid open/close cycles. Do not claim device-level verification unless actually tested.
