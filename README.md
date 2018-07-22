# Mafia

## Projekt: Petter Andersson och Robert Wörlund

2 personer A

Mobil app med Mafia

### TODO LIST:

Video 1: Game med 4 spelare
	Logga in Test 1 - 4
	Gör room, alla joinar
	alla ready sen start

	voteFordead
	accuseInstruct
	math.ceil Num
	noConfirm jämför med fel på server
	accuse vid ny dag -> updatera text


	Gör om så att lobbylist använder sockets istället för http


#### General, inte så viktig

APP activities: Alltid call super class method first

Password handling : Done ?

RecyclerView? (om den supportar lätt färgning skulle det kunna vara nice)

#### DB.sql

#### LOGIN: 

#### Menu:

Button alignment

Error text syns inte?

När man joinar ett rum som inte finns uppdaterad i listan ska den inte försöka joina/servern ska inte göra något

ConnectByPW funkar inte med MANY ROOMS atm
Testa findManyRooms update i listan

LEAVE FUNCTIONALITY,  ( TESTA MED STARTGAME, VAD HÄNDER ETC)
Kanske byta från onPause till onDestroy? (Om screen slocknar = onPause)
Game implementation kör

#### Create Room:

Load room // Inte prio atm

Error message syns inte helt, går under text

Inte tillåta samma namn på rum

#### Lobby:

READY on connect not green

FIXA SKRIV OCH SEND KNAPP LAYOUT - SER FÖR JÄVLIGT UT
Fixa så man ser det man skriver 
Gör så man ser hela chatten när man skriver // QoL(skärmen ska ner när man skriver)

LOAD LOBBY:
Endast rätt spelare får joina
Behöver att game kan startas från en specifik turn etc (startas med parametrar)

INTE PRIO - Kunna starta gamet MED CAP när alla är ready? 

#### Game:

buttons onVote -> får inte rösta isDead
prolong -> reset efter vote
båda reset vid nydag

Noone died

pause timer on vote

Disable chatt in night (send har isDay = false så funkar den inte)

Note: Layout fält namn kan ha ändras, dubbelchecka vid buggar

Gör en metod i gameClient som har userName, roomName och userChosen på samma sätt som emitter

// Instruct funkar inte, får inte instruct text från server

Hur får man list elementet man selectar?

Disabla alla funktioner när man är död

Knapparna är weird med prolong o det

Kolla vilka ui elements som ska synas på dag / natt
Lägg till en knapp för att skapa vote för accusation 
	Knappen aktiveras när votesLeft = 0
	(Om båda votes failar så får de 1 till accuse, sen går det natt)

TEST EVERYTHING
	Daytime should be buggy

// Inte så jobbig, använd timer.onCancel o kolla ett fält som antingen är postpone eller finish
Vote knapp för dag/Natt switch
Postpone day vote
TEST

// Basic
Turn / Day counter ska läggas in
Lägg in i UI.

Open Hide Chat (Använd open hide)
Chatt endast öppen om open/hide chat

Rollknapp - gör olika grejer beroende på roll, och om dag eller natt
	Mafia - Many kills for many mafias
	Cop - Bör funka
	Medic - måste implementera att man inte kan rädda samma person två gånger i rad, bör sparas på server sida

Roll infos - Ger info om alla roller, popup. frågar till databas
Får inge feedback från serversidan när den kör atm

Save / load knapp

### Databas kommunikation

#### Tabeller:
```
Roles{
	Role id, rid, int
	Rolename, roleName, string
	Instructions, instruct, String (info vad man ska göra som rollen när du är den)
	Info, info, String (information om rollen när du inte är den)
	Primary Key(rid)
}
Game{ // Removed from here when game is done
	Game id, gid, int
	Number of players, amountOfPlayers, int
	Primary Key (gid)
}
Session{
	Session id, sid, int
	Game id, gid, int	
	Turn (Which day), turn, int
	Primary Key(sid)
	Foreign Key gid REFERENCES Game (gid)
}
PlayerSession{
	Session id, sid, int
	User id, uid, int 
	Status, isAlive boolean
	Primary Key(sid, uid)
	Foreign Key sid REFERENCES Session (sid)
	Foreign Key uid REFERENCES Users(uid)
}
GameSession{
	Game id, gid, int	
	User id, uid, int 
	Role rid, rid, int 
	Primary Key(gid, uid)
	FOREIGN KEYS: gid, uid, rid REFERENCES Game(gid), Users(uid), Roles(rid)
}
Users{
	User id, uid, string (varchar)
	Username, name, string
	Password (hashat), pw, nånting (long)
	Primary Key (uid)
}

```
### Sidor: 

#### Login Screen: Logga in / Gör konto

#### Main Menu (Join Room, Create Room) 

#### Create Room (Private Game, Start->Lobby) (Dynamic roles eller set roles med set players)

#### Join Game (List of available rooms->Lobby)

#### Lobby (Host: Start, Ready check maybe, -> Game)

### Game: 	
Frontend: Day / Night time visible, Alive Players (dead), Chat, Postpone night majority, skip to night)

Backend: Spara alla användare så att man kan spara gamet. Generate roles, Send roles to players. Day / Night Messages to specific roles.

### Roller:

Mafia - Team 2: Flera mafias allowed, om flera alive väljer de target tillsammans. Behöver 3 mafias för 2 kills under en natt.

Cop - Team 1: Väljer en under natten och får information, ja eller nej, om den är team 2.

Medic - Team 1: Väljer en person att rädda under natten, får inte välja samma person två nätter i rad. 

Townfolk - Team 1: Har ingen speciell funktionalitet, är bystander.

Sheriff - Team 1: Har ett skott under hela gamet den kan skjuta under dagen

Crazy Cop - Team 1: Får reverse information vad cop får


### Game explained 
Every Night people with roles choose people (roles for day aswell)

Roles that need to cooperate, like multiple mafias do that. 
	
After night, Server update to every player with who died during night etc.

Every day people can vote on one person to be killed off, requires majority.


### Deluxe functionality:
Friendlist, databas med users som man ser om de är online
