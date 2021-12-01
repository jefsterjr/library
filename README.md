#  Springboot + ELK + Docker

Nesse artigo vamos abordar um exemplo de aplicação que utiliza o Springboot + ELK + Docker.
Se trata de uma aplicação simples, somente com intenção de demonstrar os conceitos.
Não abordarei uma explicação detalhada sobre todos os elementos, caso você não conheça essa stack, sugiro fazer uma pequena pesquisa antes de realizar esse tutorial.

## Pré Requisitos

- Ter o docker instalado em sua máquina
- Uma IDE de sua preferência (Intellij ou Eclipse)

## Mãos a obra (ou código)

### Aplicação

Começarei pela aplicação, criei uma API REST com 4 endpoints:
- GET /book/{id}
- GET /book/all
- POST /book
- DELETE /{id}

Assim ficou o Dockerfile da aplicação:

```
FROM openjdk:17.0.1 

# Diretório padrão da aplicação
WORKDIR /app

# Copiando os arquivos do maven para o /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Baixando dependências
RUN ./mvnw dependency:go-offline

# Copiando os fontes
COPY src ./src

# Compilando e executando
CMD ["./mvnw", "spring-boot:run"]

# Porta exposta para a apicação
EXPOSE 8080
```

Alternativamente você pode utilizar um Dockerfile assim:
```
# Para utilizar esse formato, você precisará empacotar sua aplicação antes (mvn package). 

FROM openjdk:17.0.1 

# Diretório padrão da aplicação
WORKDIR /app

# Copiando e renomeando o .jar
COPY ./target/*.jar ./app.jar

# Executando o jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

EXPOSE 8080
```

Como vamos usar vários containeres, vamos usar o docker compose. Na raiz do seu projeto, você criará o arquivo docker-compose.yml.

```
version: '3.2'

services:
 app:
	container_name: library_app
    build:
      context: .
    ports:
      - "8080:8088"
```
Por enquanto docker-compose.yml ficará assim.

Para testar, execute a partir da raíz do projeto: 
```
docker-compose up
```
Teste chamando os endpoint pelo Postman, navegador ou outros de sua preferência.

### Elasticsearch

Não entrarei em detalhes sobre o elasticsearch. Sugiro que pesquise sobre caso não conheça.
Para manter o projeto mais organizado e de fácil entendimento, vamos criar uma estrutura de pastas para separar os itens.
- Vamos adicionar no nosso docker-compose.yml os seguintes dados:
```
elasticsearch:
    container_name: library_elasticsearch
    image: docker.elastic.co/elasticsearch/elasticsearch:7.15.2
    volumes:
      - type: volume
        source: elasticsearch
        target: /usr/share/elasticsearch/data
    ports:
      - "9200:9200"
      - "9300:9300"
    environment:
      ES_JAVA_OPTS: "-Xmx256m -Xms256m"
      ELASTIC_PASSWORD: changeme
      discovery.type: single-node
    networks:
      - elk
networks:
  elk:
    driver: bridge
volumes:
  elasticsearch:
```
Aqui estamos definindo as configurações do Elasticsearch como porta, variáveis de memória, diretório base no docker, etc. Também criamos uma rede chama *elk* e adicionamos o nosso serviço a ela.

Execute:
```
docker-compose up
```
Acesse no navegador a url
```
http://localhost:9200/
``` 
e terá um retorno semelhante a esse: 

```json
{
  "name" : "cdd84bfdc405",
  "cluster_name" : "docker-cluster",
  "cluster_uuid" : "qiM9W35kRYuYXt-Lg61ZYg",
  "version" : {
    "number" : "7.15.2",
    "build_flavor" : "default",
    "build_type" : "docker",
    "build_hash" : "93d5a7f6192e8a1a12e154a2b81bf6fa7309da0c",
    "build_date" : "2021-11-04T14:04:42.515624022Z",
    "build_snapshot" : false,
    "lucene_version" : "8.9.0",
    "minimum_wire_compatibility_version" : "6.8.0",
    "minimum_index_compatibility_version" : "6.0.0-beta1"
  },
  "tagline" : "You Know, for Search"
}
```
### Logstash

Para o Logstash, o processo passa a ser um pouco diferente, primeiro vamos criar uma pasta chama *.logstash*, para guardarmos algumas configurações. Dentro dela criaremos o arquivo *logstash.conf* que terá as seguintes informações:
```
input {
	tcp {
		mode => "server"
		port => 4560	
		codec => json_lines
  	}
	 
	file {
            type => "java"
            path => "/var/log/logs/library/application.log"
            codec => multiline {
            pattern => "^%{YEAR}-%{MONTHNUM}-%{MONTHDAY} %{TIME}.*"
            negate => "true"
            what => "previous"
         }
    }	
}

output {
	stdout {
		codec => rubydebug
	}
	elasticsearch {
		index => "library-logstash-%{+YYYY.MM.dd}"
		hosts => "elasticsearch:9200"
		user => "elastic"
		password => "changeme"
		ecs_compatibility => disabled
	}
}
```

Aqui teremos o modo de operação, TCP ou por arquivo. No modo TCP, o logstash pegará os dados em tempo real a partir da porta especificada no arquivo *logback-spring.xml* dentro do projeto no pacote *resources*. Nós usaremos o TCP.

Arquivo logback-spring.xml:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <include resource="org/springframework/boot/logging/logback/base.xml"/>
    <springProperty scope="context" name="appName" source="spring.application.name"/>
    <property name="LOG_FILE" value="${BUILD_FOLDER:-build}/${appName}"/>
    <property name="CONSOLE_LOG_PATTERN"
              value="%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>INFO</level>
        </filter>
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>utf8</charset>
        </encoder>
    </appender>

    <appender name="LOGSTASH" class="net.logstash.logback.appender.LogstashTcpSocketAppender">
        <destination>logstash:4560</destination>
        <encoder charset="UTF-8" class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <timeZone>UTC</timeZone>
                </timestamp>
                <pattern>
                    <pattern>
                        {
                        "logLevel": "%level",
                        "serviceName": "${springAppName:-}",
                        "pid": "${PID:-}",
                        "thread": "%thread",
                        "class": "%logger{40}",
                        "rest": "%message"
                        }
                    </pattern>
                </pattern>
            </providers>
        </encoder>

        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeCallerData>true</includeCallerData>
        </encoder>
    </appender>

    <appender name="STASH" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logback/redditApp.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logback/redditApp.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="LOGSTASH"/>
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
        <appender-ref ref="STASH"/>
    </root>
</configuration>

```

Contém também a saída, que nosso caso será para o Elasticsearch conforme descrito abaixo. Nas informações de saída, definimos o destino, usuário e senha do elastic (Nesse caso está com os valores padrões), e índice. O indice servirá para podermos filtrar as informações somente dessa aplicação no kibana.

Já no docker-compose.yml, o container ficará assim:
```
  logstash:
    container_name: library_logstash
    image: docker.elastic.co/logstash/logstash:7.15.2
    volumes:
      - type: bind
        source: .logstash
        target: /usr/share/logstash/pipeline
        read_only: true
    ports:
      - "5044:5044"
      - "5000:5000/tcp"
      - "5000:5000/udp"
      - "9600:9600"
      - "4560:4560"
    environment:
      LS_JAVA_OPTS: "-Xmx256m -Xms256m"
    networks:
      - elk
    depends_on:
      - elasticsearch
```

### Kibana
Kibana possui sua configuração simples, apenas adicione ao docker-compose.yml:
```
  kibana:
    container_name: library_kibana
    image: docker.elastic.co/kibana/kibana:7.15.2
    ports:
      - "5601:5601"
    networks:
      - elk
    depends_on:
      - elasticsearch
```

Após isso, já é possível acessar o kibana pelo navegador na url: 
```
http://localhost:5601/
```

Ao utilizar o kibana, você precisará de adicionar o índice que criamos anteriormente no logstash para pegar as informações.
Para isso acesse o kibana e entre em: <http://localhost:5601/app/management/kibana/indexPatterns> ou navegue:  *Menu direita/ Stack Management/Index Patterns/ Create index pattern.*
Como exemplo, o da aplicação que citamos com exemplo ficou: **library-logstash-**
