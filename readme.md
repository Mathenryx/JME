# Linguagem JME

---
## Configuração do ambiente

### Java (Versão 11)
#### Download e Instalação do JDK (Java Development Kit):
1. Acesse o site oficial do Oracle JDK: Oracle JDK Download.
2. Baixe a versão mais recente do JDK.
3. Execute o instalador e siga as instruções para instalar o JDK.

#### Configuração das Variáveis de Ambiente:
1. Abra as "Configurações do Sistema" (procure por "Configurações Avançadas do Sistema" no menu Iniciar).
2. Clique em "Variáveis de Ambiente".
3. Em "Variáveis do Sistema", clique em "Novo" e adicione: 
   - Nome da Variável: JAVA_HOME
   - Valor da Variável: Caminho para o diretório do JDK (por exemplo, C:\Program Files\Java\jdk-11.0.12).
4. Edite a variável "Path" e adicione %JAVA_HOME%\bin no final.
5. Para verificar se o Java foi instalado corretamente, abra um terminal (prompt de comando) e execute:
`java -version`

### Maven
#### Download e Descompactação do Maven:
1. Acesse o site oficial do Apache Maven: Download do Maven.
2. Baixe a versão mais recente do Maven (arquivo zip).
3. Descompacte o arquivo zip em um diretório de sua escolha (por exemplo, C:\Program Files\Apache\maven).

#### Configuração das Variáveis de Ambiente para o Maven:
1. Adicione as seguintes variáveis de ambiente:
   - Nome da Variável: MAVEN_HOME
   - Valor da Variável: Caminho para o diretório do Maven (por exemplo, C:\Program Files\Apache\maven\apache-maven-3.8.4).
2. Edite a variável "Path" e adicione %MAVEN_HOME%\bin no final.
3. Para verificar se o Maven foi instalado corretamente, abra um terminal e execute:
`mvn -version`

**Observação:** Certifique-se de que a versão do Java é a 11.

## Build e Execução

Agora que você configurou o ambiente, siga as etapas abaixo para construir e executar o projeto:

1. Entre na pasta do projeto: `cd jme`
2. Faça o Build do Projeto: `mvn clean install`
3. Execute o projeto com o maven: `mvn exec:java -Dexec.mainClass="br.facens.Main"`

Com o projeto em execução siga os seguintes passos para compilar sua linguagem:
1. Coloque o código com extensão .jme dentro da pasta resources: `jme\src\main\resources`
2. Logo em seguida, o programa irá pedir para inserir o nome do arquivo, basta colocar o arquivo que 
foi colocado na pasta resources, exemplo: `main.jme`
3. Agora o compilador irá avisar se o código foi compilado ou se houve algum problema na compilação
