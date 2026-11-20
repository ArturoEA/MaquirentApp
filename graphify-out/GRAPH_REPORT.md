# Graph Report - MaquirentApp  (2026-11-20)

## Corpus Check
- 144 files · ~103,029 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 194 file(s) not represented in the graph (top: .xml 178, (none) 6, .ttf 3)
- WARNING: 194 of 441 files seen (44%) have no supported extractor and are missing from this graph entirely. It may look complete while representing only part of the codebase.

## Summary
- 3232 nodes · 10554 edges · 123 communities (61 shown, 62 thin omitted)
- Extraction: 85% EXTRACTED · 15% INFERRED · 0% AMBIGUOUS · INFERRED: 1605 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0959026f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Accesorio
- Callback
- androidx.recyclerview.widget.RecyclerView
- android.view.View
- android.widget.TextView
- NuevoMantenimientoFragment.java
- Mantenimiento
- NuevoMantenimientoFragment
- CertificadoOperatividad
- ItemValorizacion
- InfoPlaca
- Override
- FiltroCategoria
- NuevaCotizacionFragment
- NuevoAlquilerMensualFragment
- BasePdfGeneratorStrategy
- Valorizacion
- MantenimientosFragment
- ItemCotizacion
- GeneradorRepository
- FotoEquipo
- GrupoElectrogeno
- ListaClientesValorizacionFragment
- Plano
- TareasFragment.java
- MantenimientoConfiguracion
- Tarea
- InformacionGeneralFragment
- ViewHolder
- PagoPendiente
- FichaTecnica
- AlquilerMensual
- PdfCacheManager
- NuevoCertificadoFragment
- HistorialAlquilerMensualFragment
- ClienteValorizacion
- Cotizacion
- MantenimientosConfiguracionFragment
- ListaGruposElectrogenosFragment
- MainActivity
- CondicionComercialItem
- AuthFragment
- FichasTecnicasFragment
- NuevoAlquilerDiaFragment
- PdfViewerActivity
- GruposElectrogenosConfiguracionAdapter
- DashboardFinanzasFragment
- .getFechaFin
- HistorialCotizacionesFragment
- index.js
- Ingreso
- SaludMantenimientoFragment
- MantenimientoConfiguracionAdapter
- HistorialAlquilerDiarioFragment
- UsuarioViewHolder
- Override
- MantenimientosAdapter
- HomeFragment
- PerfilFragment
- Override
- GrupoElectrogenoFragment
- TareasAdapter
- AlquilerDia
- Usuario
- Override
- DialogGenerarValorizacion
- android.graphics.Canvas
- AlquilerDiarioAdapter
- UsuarioViewHolder
- UsuariosAdapter
- HistorialValorizacionesClienteFragment
- android.graphics.Bitmap
- .calcularHorasExtras
- ItemsMantenimientoSeleccionablesAdapter
- FotosEquipoAdapter
- android.content.Context
- android.widget.AdapterView
- CotizacionesAdapter
- FichasTecnicasAdapter
- FotosSimpleAdapter
- CalculadoraAlquilerStrategy
- .getNumeroCotizacion
- .crearDocumentoTareaRetorno
- MyFirebaseMessagingService.java
- FotosEquipoFragment
- CotizacionesRepository
- PlanosCambioVoltajeFragment
- PdfGenerator
- FichaTecnicaViewModel
- ExampleInstrumentedTest.java
- PlanoAdapter
- TareoInfo
- DaysAdapter
- MaquirentApplication.java
- ConsolidadoValorizacionesAdapter
- .getGruposActivos
- .getGrupoPorId
- MaquirentCalculadoraStrategy
- VolkerCalculadoraStrategy
- Override
- package.json
- Adapter
- CorrelativoStrategy
- MantenimientoSaludManager
- SpecsPlacaAdapter
- ResultadoHorasExtras
- dependencies
- scripts
- TareasFragment
- MaquirentCorrelativoStrategy
- PlanesMantenimientoDefaults
- VolkerCorrelativoStrategy
- limpiarTokensInvalidos
- NumeroALetras

## God Nodes (most connected - your core abstractions)
1. `Callback` - 248 edges
2. `ViewHolder` - 119 edges
3. `GrupoElectrogeno` - 111 edges
4. `Valorizacion` - 109 edges
5. `Mantenimiento` - 107 edges
6. `DetalleMes` - 99 edges
7. `MantenimientoConfiguracion` - 79 edges
8. `NuevoMantenimientoFragment` - 73 edges
9. `AlquilerMensual` - 73 edges
10. `Cotizacion` - 73 edges

## Surprising Connections (you probably didn't know these)
- `FichasTecnicasFragment` --references--> `FichasTecnicasAdapter`  [EXTRACTED]
  MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/View/FichasTecnicasFragment.java → MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/adapters/FichasTecnicasAdapter.java
- `FichasTecnicasFragment` --references--> `FichaTecnicaViewModel`  [EXTRACTED]
  MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/View/FichasTecnicasFragment.java → MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/ViewModel/FichaTecnicaViewModel.java
- `NuevoCertificadoFragment` --references--> `CertificadoOperatividad`  [EXTRACTED]
  MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/View/NuevoCertificadoFragment.java → MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/Model/CertificadoOperatividad.java
- `NuevoCertificadoFragment` --references--> `GrupoElectrogeno`  [EXTRACTED]
  MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/View/NuevoCertificadoFragment.java → MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/Model/GrupoElectrogeno.java
- `NuevoCertificadoFragment` --references--> `InfoPlaca`  [EXTRACTED]
  MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/View/NuevoCertificadoFragment.java → MAQUIRENTAPP/app/src/main/java/com/example/maquirentapp/Model/InfoPlaca.java

## Import Cycles
- None detected.

## Communities (123 total, 62 thin omitted)

### Community 0 - "Accesorio"
Cohesion: 0.06
Nodes (8): AccesoriosAdapter, AccesorioViewHolder, OnAccesorioActionListener, Accesorio, AccesoriosAlquilerDiarioFragment, AccesoriosAdapter.OnAccesorioActionListener, AccesoriosAlquilerMensualFragment, AccesoriosAdapter.OnAccesorioActionListener

### Community 1 - "Callback"
Cohesion: 0.05
Nodes (6): AlquilerRepository, Callback, MantenimientoRepository, TareaRepository, UsuarioRepository, ValorizacionesRepository

### Community 4 - "android.view.View"
Cohesion: 0.09
Nodes (3): ViewHolder, ConfiguracionFragment, ArrayAdapter<String>

### Community 5 - "android.widget.TextView"
Cohesion: 0.15
Nodes (8): ViewHolder, ViewHolder, ViewHolder, ViewHolder, VH, TareaViewHolder, ViewHolder, ViewHolder

### Community 10 - "ItemValorizacion"
Cohesion: 0.05
Nodes (8): SeleccionValorizacionAdapter, OnItemClickListener, ViewHolder, ItemValorizacion, NuevaValorizacionFragment, Callback<List<AlquilerMensual>>, Callback<List<DetalleMes>>, Callback<GrupoElectrogeno>

### Community 12 - "Override"
Cohesion: 0.06
Nodes (13): Callback<Void>, Callback<List<Accesorio>>, Callback<AlquilerMensual>, Callback<List<DetalleMes>>, TextWatcher, TextWatcher, AdapterView.OnItemSelectedListener, ItemTouchHelper.SimpleCallback (+5 more)

### Community 13 - "FiltroCategoria"
Cohesion: 0.09
Nodes (8): FiltroCategoriasAdapter, FiltroItemsAdapter.OnItemActionListener, OnCategoriaActionListener, FiltroItemsAdapter, OnItemActionListener, FiltroCategoria, FiltroItem, FiltroCategoriasAdapter.OnCategoriaActionListener

### Community 14 - "NuevaCotizacionFragment"
Cohesion: 0.10
Nodes (7): NuevaCotizacionFragment, Callback<List<GrupoElectrogeno>>, android.text.TextWatcher, android.text.TextWatcher, android.text.TextWatcher, android.text.TextWatcher, android.text.TextWatcher

### Community 16 - "BasePdfGeneratorStrategy"
Cohesion: 0.09
Nodes (3): BasePdfGeneratorStrategy, MaquirentPdfStrategy, VolkerPdfStrategy

### Community 18 - "MantenimientosFragment"
Cohesion: 0.10
Nodes (4): MantenimientosFragment, TextWatcher, com.example.maquirentapp.Repository.Callback<Integer>, ItemTouchHelper.SimpleCallback

### Community 19 - "ItemCotizacion"
Cohesion: 0.09
Nodes (5): CotizacionItemsAdapter, OnItemActionListener, ViewHolder, ItemCotizacion, CotizacionItemsAdapter.OnItemActionListener

### Community 22 - "GrupoElectrogeno"
Cohesion: 0.05
Nodes (7): MaquinaTareaAdapter, MaquinaConReporte, OnCardClickListener, ViewHolder, GrupoElectrogeno, EstadoSaludTarea, ReporteSaludEquipo

### Community 23 - "ListaClientesValorizacionFragment"
Cohesion: 0.11
Nodes (4): ListaClientesValorizacionFragment, Callback<List<ClienteValorizacion>>, Callback<List<Valorizacion>>, ItemTouchHelper.SimpleCallback

### Community 28 - "InformacionGeneralFragment"
Cohesion: 0.10
Nodes (7): InformacionGeneralFragment, FotosSimpleAdapter.OnFotoActionListener, SpecsPlacaAdapter.OnSpecActionListener, Callback<List<FiltroCategoria>>, Callback<InfoPlaca>, CustomTarget<Bitmap>, Callback<Void>

### Community 29 - "ViewHolder"
Cohesion: 0.12
Nodes (4): ViewHolder, DetalleMesAdapter, OnDetalleMesListener, DetalleMes

### Community 30 - "PagoPendiente"
Cohesion: 0.10
Nodes (3): PagoPendienteAdapter, OnItemClickListener, PagoPendiente

### Community 31 - "FichaTecnica"
Cohesion: 0.17
Nodes (3): FichaTecnica, FichaTecnicaRepository, ProgressCallback

### Community 34 - "NuevoCertificadoFragment"
Cohesion: 0.13
Nodes (4): CertificadosRepository, NuevoCertificadoFragment, Callback<InfoPlaca>, Callback<String>

### Community 35 - "HistorialAlquilerMensualFragment"
Cohesion: 0.06
Nodes (10): VH, AlquilerMensualAdapter, Callback<Accesorio>, OnItemClickListener, VH, GrupoElectrogenoAdapter, OnItemClickListener, HistorialAlquilerMensualFragment (+2 more)

### Community 37 - "ClienteValorizacion"
Cohesion: 0.11
Nodes (5): ClientesValorizacionAdapter, OnClienteActionListener, ViewHolder, ClienteValorizacion, ClientesValorizacionAdapter.OnClienteActionListener

### Community 41 - "ListaGruposElectrogenosFragment"
Cohesion: 0.07
Nodes (9): ListaGruposElectrogenosFragment, Callback<Void>, Callback<List<GrupoElectrogeno>>, Callback<GrupoElectrogeno>, Callback<Void>, GruposElectrogenosConfiguracionAdapter.OnGrupoActionListener, ItemTouchHelper.Callback, Callback<String> (+1 more)

### Community 42 - "MainActivity"
Cohesion: 0.05
Nodes (6): MainActivity, OnBackPressedCallback, Callback<Usuario>, CGEFragment, RecyclerView.ItemDecoration, Callback<List<GrupoElectrogeno>>

### Community 43 - "CondicionComercialItem"
Cohesion: 0.13
Nodes (4): CondicionesCotizacionAdapter, OnCondicionActionListener, CondicionComercialItem, CondicionesCotizacionAdapter.OnCondicionActionListener

### Community 44 - "AuthFragment"
Cohesion: 0.15
Nodes (4): AuthFragment, Callback<Void>, Callback<Usuario>, Callback<Usuario>

### Community 45 - "FichasTecnicasFragment"
Cohesion: 0.13
Nodes (4): FichasTecnicasFragment, Callback<File>, Callback<File>, FichasTecnicasAdapter.OnItemClickListener

### Community 47 - "PdfViewerActivity"
Cohesion: 0.08
Nodes (6): PdfPageAdapter, PdfViewHolder, UpdateManager, UpdateCheckListener, Loader, PdfViewerActivity

### Community 48 - "GruposElectrogenosConfiguracionAdapter"
Cohesion: 0.21
Nodes (4): GruposElectrogenosConfiguracionAdapter, GrupoViewHolder, OnGrupoActionListener, OnStartDragListener

### Community 51 - "HistorialCotizacionesFragment"
Cohesion: 0.17
Nodes (3): HistorialCotizacionesFragment, Callback<List<Cotizacion>>, ItemTouchHelper.SimpleCallback

### Community 52 - "index.js"
Cohesion: 0.09
Nodes (13): admin, ADMIN_EMAIL, borrarFotoPorUrl(), borrarFotosDeGrupoEliminado(), { defineSecret }, EMAIL_PASS, EMAIL_USER, { getStorage } (+5 more)

### Community 54 - "SaludMantenimientoFragment"
Cohesion: 0.15
Nodes (7): SaludMantenimientoFragment, Callback<ReporteSaludEquipo>, FiltroEstado, AL_DIA, PROXIMAS, TODAS, VENCIDAS

### Community 55 - "MantenimientoConfiguracionAdapter"
Cohesion: 0.16
Nodes (5): MantenimientoConfiguracionAdapter, FilaVisual, HeaderViewHolder, MantenimientoConfiguracionViewHolder, OnMantenimientoConfiguracionActionListener

### Community 56 - "HistorialAlquilerDiarioFragment"
Cohesion: 0.17
Nodes (3): HistorialAlquilerDiarioFragment, Callback<List<AlquilerDia>>, ItemTouchHelper.SimpleCallback

### Community 58 - "Override"
Cohesion: 0.11
Nodes (7): Callback<List<Accesorio>>, Callback<AlquilerDia>, Callback<List<GrupoElectrogeno>>, AdapterView.OnItemSelectedListener, Callback<AlquilerDia>, Callback<Void>, Callback<MantenimientoSaludManager.ResultadoSalud>

### Community 59 - "MantenimientosAdapter"
Cohesion: 0.10
Nodes (4): MantenimientosAdapter, Filter, OnMantenimientoClickListener, ViewHolder

### Community 60 - "HomeFragment"
Cohesion: 0.11
Nodes (6): HomeFragment, Callback<List<Accesorio>>, Callback<List<AlquilerMensual>>, Callback<List<DetalleMes>>, Callback<List<AlquilerDia>>, Callback<List<GrupoElectrogeno>>

### Community 62 - "Override"
Cohesion: 0.15
Nodes (5): Callback<Void>, Callback<List<FichaTecnica>>, Callback<File>, Callback<Void>, Callback<Void>

### Community 63 - "GrupoElectrogenoFragment"
Cohesion: 0.22
Nodes (3): GrupoElectrogenoFragment, Callback<List<Ingreso>>, AdapterView.OnItemSelectedListener

### Community 67 - "Override"
Cohesion: 0.15
Nodes (6): Callback<ClienteValorizacion>, Callback<GrupoElectrogeno>, Callback<InfoPlaca>, Callback<String>, TextWatcher, Callback<ClienteValorizacion>

### Community 70 - "AlquilerDiarioAdapter"
Cohesion: 0.11
Nodes (4): AlquilerDiarioAdapter, OnAlquilerDiaClickListener, Callback<Accesorio>, RecyclerView.ItemDecoration

### Community 71 - "UsuarioViewHolder"
Cohesion: 0.17
Nodes (4): Callback<Void>, Callback<Void>, Callback<Void>, Callback<Void>

### Community 72 - "UsuariosAdapter"
Cohesion: 0.21
Nodes (4): UsuariosAdapter, OnUsuarioActionListener, GestionarUsuariosFragment, Callback<List<Usuario>>

### Community 73 - "HistorialValorizacionesClienteFragment"
Cohesion: 0.12
Nodes (6): ValorizacionesAdapter, OnItemClickListener, ViewHolder, HistorialValorizacionesClienteFragment, Callback<List<Valorizacion>>, ItemTouchHelper.SimpleCallback

### Community 76 - "ItemsMantenimientoSeleccionablesAdapter"
Cohesion: 0.17
Nodes (3): ItemsMantenimientoSeleccionablesAdapter, FilaVisual, HeaderViewHolder

### Community 77 - "FotosEquipoAdapter"
Cohesion: 0.27
Nodes (3): FotosEquipoAdapter, OnFotoClickListener, ViewHolder

### Community 78 - "android.content.Context"
Cohesion: 0.14
Nodes (3): PdfGeneratorStrategy, OnDateSetListener, MantenimientoOfflineManager

### Community 80 - "CotizacionesAdapter"
Cohesion: 0.27
Nodes (3): CotizacionesAdapter, OnCotizacionClickListener, ViewHolder

### Community 82 - "FotosSimpleAdapter"
Cohesion: 0.22
Nodes (4): FotosSimpleAdapter, AgregarViewHolder, FotoViewHolder, OnFotoActionListener

### Community 84 - ".getNumeroCotizacion"
Cohesion: 0.18
Nodes (3): PdfGenerator.OnPdfGeneratedListener, Callback<String>, Callback<Void>

### Community 90 - "PdfGenerator"
Cohesion: 0.19
Nodes (4): PdfGenerator, WebViewClient, WebViewClient, OnPdfGeneratedListener

### Community 93 - "PlanoAdapter"
Cohesion: 0.31
Nodes (3): PlanoAdapter, OnPlanoClickListener, ViewHolder

### Community 94 - "TareoInfo"
Cohesion: 0.32
Nodes (3): CalendarioTareoHelper, MesSpan, TareoInfo

### Community 95 - "DaysAdapter"
Cohesion: 0.29
Nodes (3): DayItem, DaysAdapter, DayViewHolder

### Community 97 - "ConsolidadoValorizacionesAdapter"
Cohesion: 0.27
Nodes (3): ConsolidadoValorizacionesAdapter, OnValorizacionClickListener, ViewHolder

### Community 103 - "package.json"
Cohesion: 0.22
Nodes (9): description, devDependencies, firebase-functions-test@^3.1.0, engines, node, main, name, private (+1 more)

### Community 107 - "CorrelativoStrategy"
Cohesion: 0.13
Nodes (4): CorrelativoStrategy, HtmlGenerator, StrategyProvider, StrategyProvider

### Community 110 - "MantenimientoSaludManager"
Cohesion: 0.17
Nodes (10): MantenimientoSaludManager, EstadoSalud, OPTIMO, PROXIMO, VENCIDO_HORAS, VENCIDO_TIEMPO, MetricasPuntualidad, PuntoHistorialIntervalo (+2 more)

### Community 112 - "SpecsPlacaAdapter"
Cohesion: 0.29
Nodes (3): SpecsPlacaAdapter, OnSpecActionListener, ViewHolder

### Community 115 - "dependencies"
Cohesion: 0.29
Nodes (7): dependencies, firebase-admin@^12.6.0, firebase-functions@^6.0.1, nodemailer@^7.0.10, firebase-admin, firebase-functions, nodemailer

### Community 120 - "scripts"
Cohesion: 0.33
Nodes (6): scripts, deploy, logs, serve, shell, start

### Community 121 - "TareasFragment"
Cohesion: 0.15
Nodes (4): OnRegistrarMantenimientoListener, TareasFragment, Callback<List<GrupoElectrogeno>>, Callback<List<MantenimientoConfiguracion>>

### Community 132 - "limpiarTokensInvalidos"
Cohesion: 0.67
Nodes (3): limpiarTokensInvalidos(), notificarNuevoAlquilerMensual(), notificarPagosPendientes()

## Knowledge Gaps
- **8 isolated node(s):** `TODAS`, `VENCIDAS`, `PROXIMAS`, `AL_DIA`, `OPTIMO` (+3 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 375 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **62 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Callback` connect `Callback` to `list`, `androidx.recyclerview.widget.RecyclerView`, `android.view.View`, `android.widget.TextView`, `NuevoMantenimientoFragment.java`, `CertificadoOperatividad`, `ItemValorizacion`, `Override`, `FiltroCategoria`, `NuevaCotizacionFragment`, `NuevoAlquilerMensualFragment`, `Valorizacion`, `MantenimientosFragment`, `ItemCotizacion`, `GeneradorRepository`, `FotoEquipo`, `GrupoElectrogeno`, `ListaClientesValorizacionFragment`, `Plano`, `MantenimientoConfiguracion`, `InformacionGeneralFragment`, `ViewHolder`, `FichaTecnica`, `AlquilerMensual`, `PdfCacheManager`, `.mostrarDialogoEditarPeriodo`, `Cotizacion`, `.bind`, `ListaGruposElectrogenosFragment`, `MainActivity`, `AuthFragment`, `FichasTecnicasFragment`, `NuevoAlquilerDiaFragment`, `GruposElectrogenosConfiguracionAdapter`, `.getFechaFin`, `HistorialCotizacionesFragment`, `Ingreso`, `SaludMantenimientoFragment`, `HistorialAlquilerDiarioFragment`, `UsuarioViewHolder`, `HomeFragment`, `Override`, `GrupoElectrogenoFragment`, `AlquilerDia`, `Usuario`, `Override`, `UsuarioViewHolder`, `UsuariosAdapter`, `HistorialValorizacionesClienteFragment`, `.calcularHorasExtras`, `android.content.Context`, `FotosEquipoFragment`, `CotizacionesRepository`, `PlanosCambioVoltajeFragment`, `FichaTecnicaViewModel`, `.getGruposActivos`, `.getGrupoPorId`, `TareasFragment`?**
  _High betweenness centrality (0.181) - this node is a cross-community bridge._
- **Are the 6 inferred relationships involving `Callback` (e.g. with `.cargarPendientes()` and `.onSuccess()`) actually correct?**
  _`Callback` has 6 INFERRED edges - model-reasoned connections that need verification._
- **What connects `TODAS`, `VENCIDAS`, `PROXIMAS` to the rest of the system?**
  _8 weakly-connected nodes found - possible missing edges._
- **Should `Accesorio` be split into smaller, more focused modules?**
  _Cohesion score 0.055525606469002696 - nodes in this community are weakly interconnected._
- **Why does `GrupoElectrogeno` connect `GrupoElectrogeno` to `Accesorio`, `list`, `androidx.recyclerview.widget.RecyclerView`, `android.view.View`, `android.widget.TextView`, `NuevoMantenimientoFragment.java`, `ItemValorizacion`, `NuevaCotizacionFragment`, `Tarea`, `NuevoCertificadoFragment`, `HistorialAlquilerMensualFragment`, `ListaGruposElectrogenosFragment`, `MainActivity`, `NuevoAlquilerDiaFragment`, `GruposElectrogenosConfiguracionAdapter`, `Override`, `HomeFragment`, `GrupoElectrogenoFragment`, `.getGruposActivos`, `.getGrupoPorId`?**
  _High betweenness centrality (0.064) - this node is a cross-community bridge._
- **Should `Callback` be split into smaller, more focused modules?**
  _Cohesion score 0.052417006406523005 - nodes in this community are weakly interconnected._
- **Why does `Mantenimiento` connect `Mantenimiento` to `Callback`, `list`, `androidx.recyclerview.widget.RecyclerView`, `android.widget.TextView`, `NuevoMantenimientoFragment.java`, `android.graphics.Canvas`, `NuevoMantenimientoFragment`, `.getFechaMantenimientoMillis`, `android.content.Context`, `MantenimientoSaludManager`, `BasePdfGeneratorStrategy`, `MantenimientosFragment`, `GrupoElectrogeno`, `MantenimientosAdapter`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._