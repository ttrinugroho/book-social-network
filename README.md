# Book Social Network
This is a project to learn and practice the lessons of course Fullstack Java Springboot and Angular.

## Run application
```bash
# run posgresql server and mail dev in docker
docker-composer up -d

# run Java Springboot as Backend
cd book-networks && ./mvnw spring-boot:run

# run angular
cd book-network-ui && npm run start
```

## Host and port application running
* `localhost:1080` : Maildev application
* `localhost:5432` : PosgreSql server
* `localhost:8088/api/v1` : Springboot Backend Application
* `localhost:4200` : Angular Frontend Application