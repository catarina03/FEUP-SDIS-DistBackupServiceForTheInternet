# SDIS Project

Second project developed during the Distributed Systems curricular unit @ FEUP.  
All code written in collaboration with [Bernardo Ramalho](https://github.com/BernardoRamalho), [Flávia Carvalhido](https://github.com/flaviacarvalhido) and [João Rosário](https://github.com/Deadrosas).

## General Info

- **Date:** 2nd semester of 3rd year - 2020/2021
- **Curricular unit page:** [SDIS](https://sigarra.up.pt/feup/en/UCURR_GERAL.FICHA_UC_VIEW?pv_ocorrencia_id=459489)

## Distributed Backup Service  

In this project we developed a peer-to-peer distributed backup service for the Internet.   
The idea was to use the free disk space of the computers on the Internet for backing up files in one's own computer. As in the [first project](https://github.com/catarina03/FEUP-SDIS-DistBackupService), the service supports the backup, restore and deletion of files.   
Also, the participants in the service retain total control over their own storage, and therefore they may delete copies of files that they have previously stored.  
**Final grade:** 16.86/20 


## Useful resources

- [Full project specification](https://paginas.fe.up.pt/~pfs/aulas/sd2021/projs/proj2/proj2.html)
- [Final report](./doc/report.pdf)

## Instructions:

1. Make sure that you are in the **src** directory;

2. Run:   
```start rmiregistry```   
```javac *.java```   

3. Open one terminal for each peer and run for each:   
```java ChordPeer <"node-address"> <"node-port"> <"rmi-name"> (<"known-node-addres"> <"known-node-port">)?```

4. Open other terminal and run:   
```java TestApp <"rmi-name"> <"OPERATION"> (<"ARGUMENTS>)*```

5. Repeat 4 as many times as you like.