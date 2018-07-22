use worlund; # Byt till din egen

/*
mysql --host=mysql-vt2016.csc.kth.se --user=worlund_admin --password=yCrFMc5j

drop table securities; # Radera om redan finns
drop table orders;
drop table trades;
*/
drop table roles;
drop table game;
drop table session;
drop table playerSession;
drop table gameSession;
drop table users;


create table roles(
       rid int NOT NULL,
       roleName varchar(64),
       instruct TEXT,
       info TEXT,
       PRIMARY KEY (rid)
);

create table game(
       gid int NOT NULL AUTO_INCREMENT,
       roomName varchar(64),
       amountOfPlayers int, 
       PRIMARY KEY(gid)
);

create table session(
       sid int NOT NULL AUTO_INCREMENT,
       gid int,
       turn int,
       PRIMARY KEY(sid),
       FOREIGN KEY (gid) REFERENCES game(gid)
);

create table playerSession(
       sid int,
       uid int,
       isAlive boolean,
       PRIMARY KEY(sid,uid),
       FOREIGN KEY (sid) REFERENCES session(sid),
       FOREIGN KEY (uid) REFERENCES users(uid)
);

create table gameSession(
       gid int,
       uid int,
       rid int,
       PRIMARY KEY(gid, uid),
       FOREIGN KEY (gid) REFERENCES game(gid),
       FOREIGN KEY (uid) REFERENCES users(uid),
       FOREIGN KEY (rid) REFERENCES roles(rid)
);

create table users(
       uid int NOT NULL AUTO_INCREMENT,
       name varchar(64) NOT NULL,
       pw CHAR(60) NOT NULL, /**/
       PRIMARY KEY(uid)
       /*UNIQUE NAME*/
);

INSERT INTO roles(rid, roleName, instruct, info) VALUES (1, 'Mafia', 'You are a Mafia! Every night you are suppose to kill someone in the town untill you are a majority in town', 'Mafia is the villain in the game. Its goal is to kill all the players in the game who are not Mafia');
INSERT INTO roles(rid, roleName, instruct, info) VALUES (2, 'Cop', 'You are a Cop! Investigate people in the night to find out their who they are', 'The cop is able to investigate people at night in order to find who is Mafia');
INSERT INTO roles(rid, roleName, instruct, info) VALUES (3, 'Medic', 'You are a Medic! Save a person at night so they cannot get killed during that night, do not select same person two times in a row tho.', 'The medic targets a player at Night in order to protect that player from being killed during the night, cannot select the same player two consecutive nights in a row');
INSERT INTO roles(rid, roleName, instruct, info) VALUES (4, 'Townfolk', 'You are Townfolk! Chill and be happy, do not get killed lul confuse those mafiozos', 'Townsfolk has no extraordinary abillities, their goal is to stay alive to win over Mafias');
INSERT INTO roles(rid, roleName, instruct, info) VALUES (5, 'Sheriff', 'You are Sheriff! You have one bullet, use it wisely to kill one of the bad guys', 'Sheriff has one bullet that can be shot during the day, in order to try to kill the Mafia');
INSERT INTO roles(rid, roleName, instruct, info) VALUES (6, 'Crazy Cop', 'You are a Cop! Investigate people in the night to find out their who they are', 'Crazy cop gets the opposite results as the normal sane cop would get, does not know his sanity to begin with');


/*
Mafia - Team 2: Flera mafias allowed, om flera alive väljer de target tillsammans. Behöver 3 mafias för 2 kills under en natt.

Cop - Team 1: Väljer en under natten och får information, ja eller nej, om den är team 2.

Medic - Team 1: Väljer en person att rädda under natten, får inte välja samma person två nätter i rad. 

Townfolk - Team 1: Har ingen speciell funktionalitet, är bystander.

Sheriff - Team 1: Har ett skott under hela gamet den kan skjuta under dagen

Crazy Cop - Team 1: Får reverse information vad cop får
*/