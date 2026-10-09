# AtravesarPlantas

Plugin para **Paper 26.x** que te deja **golpear** mobs e **interactuar** (clic derecho)
con cofres y entidades a traves de pasto, flores, helechos, pasto alto y cualquier planta
(de 1 o 2 bloques de alto) que en vanilla te bloquea el click.

## Que hace

**Clic izquierdo:** golpea al mob que esta detras de la planta (sin romperla).

**Clic derecho a traves de plantas:**
- Abrir: cofres, cofres atrapados, barriles, shulkers, hornos (todos), tolvas,
  dispensadores, droppers, soportes de pociones, cofre de ender y mesa de crafteo.
- Entidades: comerciar con aldeanos y comerciante errante, montar botes y vagonetas,
  abrir vagonetas con cofre o tolva.
- Respeta plugins de proteccion (WorldGuard, claims, etc.).
- Agachado (shift) desactiva la funcion para que puedas colocar bloques normalmente.

**Limitaciones:** la API de Paper no tiene una "interaccion generica", asi que las
acciones que dependen del item en la mano (alimentar, esquilar, ordenar, poner nametag,
montar caballos, etc.) y puertas/palancas/botones no estan incluidas.

## Como obtener el .jar (sin instalar nada)

1. Crea un repositorio en GitHub y sube todos los archivos de esta carpeta
   (incluida la carpeta oculta `.github`).
2. Ve a la pestana **Actions**. El workflow **Build** se ejecuta solo al subir.
3. Cuando termine (check verde), entra a la ejecucion y descarga el artifact
   **AtravesarPlantas** (es un zip con el `.jar` dentro).

### Alternativa: Releases
Crea un tag que empiece con `v` (por ejemplo `v1.1.0`) y el .jar aparecera
automaticamente en la seccion **Releases** del repositorio.

## Instalacion
Copia el `.jar` a la carpeta `plugins/` de tu servidor Paper y reinicia.

## Configuracion (`plugins/AtravesarPlantas/config.yml`)
| Opcion | Default | Descripcion |
|---|---|---|
| `reach` | `3.0` | Alcance maximo del golpe |
| `ray-size` | `0.0` | Margen extra del hitbox |
| `protect-plant` | `true` | No romper la planta al golpear al mob |
| `allow-players` | `true` | Permite golpear jugadores tambien |
| `interact.enabled` | `true` | Activa el clic derecho a traves de plantas |
| `interact.block-reach` | `4.5` | Alcance para abrir bloques |
| `interact.entity-reach` | `3.0` | Alcance para interactuar con entidades |

Comando: `/atravesarplantas reload` (permiso `atravesarplantas.admin`).

Permisos (todos por defecto, salvo admin):
- `atravesarplantas.use` - golpear a traves de plantas
- `atravesarplantas.interact` - clic derecho a traves de plantas
- `atravesarplantas.admin` - recargar config

## Compatibilidad / version de la API
El `pom.xml` compila contra `paper.api.version` (por defecto `26.1.2.build.67-stable`)
y el plugin usa solo API estable, asi que deberia cargar en 26.2 y 26.3.
Si quieres compilar contra una version especifica, cambia esa propiedad en el
`pom.xml` por la que aparezca en https://repo.papermc.io/ para `io.papermc.paper:paper-api`.
Requiere **Java 25**.
