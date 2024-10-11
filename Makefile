# Variables
SOURCES = src/main/java
RESOURCES = src/main/resources
CLASSPATH = target
BINDIR = bin
LOGDIR = log

JAVA_TARGET = 21
MAIN_CLASS = com.ass2.Client
SPREAD_CONF = $(RESOURCES)/spread.conf

CLIENT_JAR = $(BINDIR)/client.jar

DEPS = ext/spread.jar

JAVA_SOURCES = $(shell find $(SOURCES) -name '*.java')
CLASS_FILES = $(patsubst $(SOURCES)/%.java,$(CLASSPATH)/%.class,$(JAVA_SOURCES))

SPREAD_SERVER_ADDRESS = 127.0.0.1
ACCOUNT_NAME = group07
REPLICAS = 3

$(LOGDIR):
	@mkdir -p $(LOGDIR)

$(CLASSPATH):
	@mkdir -p $(CLASSPATH)

$(BINDIR):
	@mkdir -p $(BINDIR)

$(CLASSPATH)/%.class: $(SOURCES)/%.java | $(CLASSPATH)
	javac --target $(JAVA_TARGET) -d $(CLASSPATH) -cp $(CLASSPATH):$(DEPS) --source-path $(SOURCES) $<

classfiles: $(CLASS_FILES)

clean:
	rm -rf $(CLASSPATH)

purge: clean
	rm -rf $(BINDIR)/*.jar
	rm -rf $(LOGDIR)/*.log*

$(CLIENT_JAR): classfiles | $(BINDIR)
	jar cfm $(CLIENT_JAR) $(RESOURCES)/MANIFEST.MF -C $(CLASSPATH) .


run_interactive: $(CLIENT_JAR)
	java -cp $(CLIENT_JAR):$(DEPS) $(MAIN_CLASS) $(SPREAD_SERVER_ADDRESS) $(ACCOUNT_NAME) $(REPLICAS)

run_simulation: $(CLIENT_JAR)
	java -cp $(CLIENT_JAR):$(DEPS) $(MAIN_CLASS) $(SPREAD_SERVER_ADDRESS) $(ACCOUNT_NAME) $(REPLICAS) $(RESOURCES)/commands.txt


all: classfiles $(CLIENT_JAR)


.PHONY: all clean purge run_client run_batch
.DEFAULT_GOAL := classfiles

