---
tags: [Java, TPO, OOP, RPG, GameDev]
title: Dungeon Tales - Descripción del Proyecto
---

# [cite_start]DUNGEON TALES [cite: 1]
[cite_start]**Descripción general del proyecto** [cite: 2]
**Paradigma Orientado a Objetos — TPO | [cite_start]Fase A** [cite: 3]

---

## [cite_start]¿De qué se trata el juego? [cite: 4]
[cite_start]Dungeon Tales es un juego de rol por turnos ambientado en un calabozo de fantasía oscura[cite: 5]. [cite_start]La idea central es simple: armás una party de tres héroes y los llevás a través de un mapa lleno de peligros, combates y decisiones, hasta enfrentarte al jefe final del nivel[cite: 6]. 

[cite_start]No hay exploración libre ni mundo abierto[cite: 7]. Todo el juego gira en torno a dos mecánicas principales:
* Navegar el mapa eligiendo qué camino tomar en cada bifurcación[cite: 8].
* [cite_start]Pelear por turnos usando las habilidades de cada personaje de manera estratégica[cite: 9].

[cite_start]La estética y el tono están inspirados en *Darkest Dungeon 2*: pantallas oscuras, personajes con barras de vida visibles todo el tiempo, y una sensación de que cada decisión tiene peso[cite: 10]. [cite_start]Si tu party muere, se acabó[cite: 11].

---

## [cite_start]¿De dónde sacamos la idea? [cite: 12]
[cite_start]El juego mezcla tres referentes que al grupo nos gustaron mucho[cite: 13]:

### [cite_start]1. Darkest Dungeon 2 — Estética y combate [cite: 14]
* De acá tomamos la idea visual: personajes e enemigos enfrentados en pantalla, con barras de HP siempre visibles, efectos de estado como veneno o aturdimiento, y esa sensación de tensión oscura[cite: 15]. 
* [cite_start]El combate no es un menú genérico, es una pantalla propia con peso visual[cite: 16].

### [cite_start]2. Clair Obscur: Expedition 33 — Sistema de PA [cite: 17]
* Este juego tiene un sistema de Puntos de Acción (PA) que nos pareció muy interesante para agregar estrategia[cite: 18]. 
* [cite_start]En vez de hacer una sola acción por turno, cada personaje tiene un *pool* de PA que se recupera al inicio de su turno[cite: 19]. 
* [cite_start]Los ataques básicos cuestan 2 PA, las habilidades especiales cuestan entre 2 y 5 PA[cite: 20]. 
* Esto crea decisiones reales: ¿uso mi habilidad cara ahora o la guardo para el próximo turno? [cite: 21]

### 3. Slay the Spire — Mapa de nodos [cite: 22]
* [cite_start]En vez de un mundo lineal, el mapa funciona como un árbol con bifurcaciones[cite: 23]. 
* [cite_start]Después de cada encuentro, el jugador ve dos o tres opciones de adónde ir: puede ir a otro combate, a un descanso para curar HP, a una tienda para gastar oro, o arriesgarse por un nodo élite con mejor recompensa[cite: 24]. 
* Esta elección de ruta le da variedad y rejugabilidad al juego[cite: 25].

---

## Los Personajes [cite: 26]
La party está compuesta por tres héroes de Dungeons & Dragons, cada uno con un rol claro en el combate[cite: 27]:

* [cite_start]**Rogue — Kira:** [cite: 28] 
  * [cite_start]El personaje más rápido del grupo[cite: 29]. 
  * Actúa primero en casi todos los turnos gracias a su velocidad alta (20)[cite: 29]. 
  * [cite_start]No tiene mucha vida ni defensa, pero hace daño elevado y puede envenenar enemigos o activar evasión para esquivar ataques[cite: 30]. 
  * [cite_start]Sus cuatro habilidades son: Golpe Furtivo (hace el doble de daño si el enemigo ya está envenenado), Envenenar, Evasión y Doble Ataque[cite: 31].
* **Paladín — Aldric:** [cite: 32] 
  * [cite_start]El tanque y soporte de la party[cite: 33]. 
  * [cite_start]Tiene la mayor defensa del grupo y puede sanar aliados con Imposición de Manos[cite: 33]. 
  * Su ataque especial Golpe Divino ignora la defensa del enemigo, haciendo daño sagrado puro[cite: 34]. 
  * [cite_start]También puede protegerse con Escudo de Fe o castigar a todos los enemigos a la vez con Castigo Sagrado[cite: 35].
* [cite_start]**Guerrero — Bronn:** [cite: 36] 
  * El que más pega[cite: 37]. 
  * [cite_start]Tiene el HP más alto (130) y sus habilidades son puro daño o control[cite: 37]. 
  * [cite_start]Golpe Brutal hace 3 veces su ataque base[cite: 38]. 
  * Carga aturde al enemigo haciéndolo perder su turno[cite: 38]. 
  * [cite_start]Grito de Batalla sube el ataque de toda la party[cite: 39]. 
  * [cite_start]Torbellino golpea a todos los enemigos al mismo tiempo[cite: 39].

> [cite_start]**Nota de Sinergia:** La idea de fondo es que los tres personajes se complementan: el Rogue envenena y debilita, el Guerrero aturde y revienta, y el Paladín mantiene a todos vivos[cite: 40].

---

## [cite_start]Los Enemigos [cite: 41]
[cite_start]Hay tres tipos de enemigos normales y un jefe de nivel[cite: 42]. [cite_start]Cada uno tiene comportamiento diferente[cite: 42]:

1. [cite_start]**Goblin Acechador:** Rápido y oportunista[cite: 43]. [cite_start]Ataca dos veces seguidas con frecuencia[cite: 43]. [cite_start]Fácil de matar pero puede hacerse el difícil en grupos[cite: 44].
2. [cite_start]**Esqueleto Guerrero:** Más lento pero resistente[cite: 45]. [cite_start]Alterna entre ataques básicos, golpes fuertes y posturas defensivas[cite: 45].
3. [cite_start]**Troll de Piedra (Élite):** Se regenera HP cada turno[cite: 46]. [cite_start]Si no lo matás rápido, se vuelve muy difícil de bajar[cite: 47]. [cite_start]Su rugido baja el ataque de toda la party[cite: 47].
4. [cite_start]**La Sombra Ancestral (Jefe Final):** Más HP, más daño, puede invocar Esqueletos como refuerzo[cite: 48].

---

## [cite_start]Las pantallas del juego [cite: 49]
[cite_start]A continuación explicamos qué muestra cada una de las capturas de pantalla del prototipo de interfaz que diseñamos[cite: 50].

### [cite_start]`Aa-TPO-ParadigmaObjetos/imagenes/menu.jpeg` — Pantalla de inicio [cite: 51]
* Esta es la primera pantalla que ve el jugador al abrir el juego[cite: 53]. 
* [cite_start]Tiene un estilo muy minimalista y oscuro, con el nombre del juego centrado en letras grandes y tres opciones: Nueva Partida, Cargar Partida y Opciones[cite: 54]. 
* [cite_start]La jerarquía visual es clara: el título domina, las opciones están debajo con distintos niveles de énfasis (Nueva Partida resalta más que las otras dos)[cite: 55]. 
* El tono oscuro y la tipografía con espaciado amplio ya comunican la estética de calabozo que queremos antes de que el jugador haga nada[cite: 56]. 
* [cite_start]En la versión final, el fondo tendría una ilustración sutil o un efecto de niebla animado[cite: 57]. [cite_start]Por ahora el wireframe muestra la estructura y jerarquía[cite: 58].

### [cite_start]`Aa-TPO-ParadigmaObjetos/imagenes/mapa.jpeg` — Pantalla del mapa de nodos [cite: 59]
* [cite_start]Esta pantalla aparece después de cada batalla[cite: 61]. 
* Muestra el mapa del nivel en formato árbol, de abajo hacia arriba: el jugador empieza en el nodo START (abajo al centro) y va avanzando hasta llegar al BOSS (arriba)[cite: 61]. 
* [cite_start]Cada nodo tiene un color y etiqueta que indica su tipo[cite: 62]:
  * [cite_start]**Rojo [COMB]:** Combate normal contra enemigos aleatorios[cite: 63].
  * **Amarillo [ELIT]:** Enemigo élite, más difícil pero da el doble de recompensa[cite: 64].
  * [cite_start]**Azul [REST]:** Campamento[cite: 65]. [cite_start]Podés curar HP de la party o meditar[cite: 65].
  * **Violeta [SHOP]:** Tienda para gastar el oro acumulado en pociones[cite: 66].
  * [cite_start]**Verde [TESO]:** Tesoro gratuito: EXP, oro o curación aleatoria[cite: 67].
  * [cite_start]**Rojo brillante [BOSS]:** El jefe final del nivel[cite: 68].
* Las líneas punteadas conectan los nodos mostrando qué caminos están disponibles[cite: 69]. El jugador solo puede ir hacia adelante, nunca retroceder[cite: 69]. 
* [cite_start]En la parte superior de la pantalla hay un resumen rápido del HP actual de cada personaje de la party[cite: 70]. 
* [cite_start]La idea de fondo es que el jugador tome decisiones con información: si la party está baja de vida, conviene priorizar un nodo REST antes de seguir peleando[cite: 71].

### [cite_start]`Aa-TPO-ParadigmaObjetos/imagenes/combate.jpeg` — Pantalla de batalla [cite: 72]
[cite_start]Esta es la pantalla más importante del juego y donde se pasa la mayor parte del tiempo[cite: 74]. [cite_start]Está dividida en tres zonas[cite: 75]:
1. [cite_start]**Zona izquierda (La party):** Muestra una *card* por cada personaje vivo[cite: 76]. [cite_start]Cada card tiene el nombre, la clase, dos barras (HP en rojo y PA en azul) con los valores numéricos, las estadísticas de ATK y DEF, y los efectos de estado activos (veneno, escudo, evasión, etc.)[cite: 77]. [cite_start]La card del personaje que está actuando tiene un borde dorado para destacarse[cite: 78].
2. [cite_start]**Zona derecha (Los enemigos):** Cards similares para cada enemigo, con su HP actual y una línea que muestra qué acción planea hacer en su próximo turno (esta información le da al jugador la chance de prepararse o priorizar a quién atacar)[cite: 79].
3. [cite_start]**Zona inferior (Menú de acciones):** Aparece cuando es el turno de un personaje del jugador[cite: 80]. [cite_start]Muestra todas las acciones disponibles con su costo en PA[cite: 81]. [cite_start]Si el personaje no tiene suficientes PA para una habilidad, esa opción aparece apagada[cite: 82]. [cite_start]También tiene el botón de Usar Poción y Terminar Turno[cite: 83].
* [cite_start]El indicador de turno en la parte superior muestra el orden completo de acción para ese turno, ordenado por velocidad[cite: 84]. [cite_start]Así el jugador siempre sabe quién va después[cite: 85].

### [cite_start]`Aa-TPO-ParadigmaObjetos/imagenes/postBatalla.jpeg` — Pantalla de resultados [cite: 86]
[cite_start]Esta pantalla aparece al terminar una batalla, ya sea con victoria o derrota[cite: 88]. [cite_start]En caso de victoria muestra[cite: 88]:
* El EXP ganado y el Oro obtenido de los enemigos derrotados[cite: 89].
* [cite_start]Si algún personaje subió de nivel, aparece resaltado con un indicador especial[cite: 90].
* [cite_start]Si se encontró algún ítem como recompensa, se muestra con su nombre y rareza[cite: 91].
* El estado actual de cada personaje de la party: HP restante con su barra y nivel actual[cite: 92].
* [cite_start]Desde esta pantalla el jugador puede elegir entre continuar al mapa (para elegir el próximo nodo) o abrir el inventario a revisar los ítems[cite: 93]. 

[cite_start]En caso de derrota total de la party, en cambio, aparece la pantalla de *Game Over* con el mensaje correspondiente[cite: 94]. [cite_start]La idea de esta pantalla es que el jugador sienta el progreso: ver los números subir, los niveles mejorar y el oro acumularse es parte de la satisfacción del género RPG[cite: 95].

---

## [cite_start]¿En qué estado está el proyecto? [cite: 96]
[cite_start]Actualmente tenemos completa la Fase A del trabajo[cite: 97]:
* [cite_start]El diagrama de clases con todas las entidades del sistema y sus relaciones[cite: 98].
* Dos diagramas de secuencia: el flujo de combate y el flujo de navegación del mapa[cite: 99].
* [cite_start]Las cuatro pantallas de interfaz diseñadas como *wireframes* con la estética final[cite: 100].
* [cite_start]Un prototipo funcional en Java de terminal que ya corre el juego completo: mapa, combate, descanso, tienda y tesoro[cite: 101].

[cite_start]Para la **Fase B (junio)** el plan es reemplazar la interfaz de terminal con JavaFX, agregando las pantallas visuales, animaciones de combate y persistencia en archivo para guardar y cargar partidas[cite: 102].