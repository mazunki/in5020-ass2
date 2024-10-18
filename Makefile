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
COMMAND_FILE = $(RESOURCES)/commands$(N).txt # Default to commandsN.txt where N is passed externally

# Create directories if they don't exist
$(LOGDIR):
	@mkdir -p $(LOGDIR)

$(CLASSPATH):
	@mkdir -p $(CLASSPATH)

$(BINDIR):
	@mkdir -p $(BINDIR)

# Compile Java source files
$(CLASSPATH)/%.class: $(SOURCES)/%.java | $(CLASSPATH)
	javac --target $(JAVA_TARGET) -d $(CLASSPATH) -cp $(CLASSPATH):$(DEPS) --source-path $(SOURCES) $<

# Create all class files
classfiles: $(CLASS_FILES)

# Clean class and log files
clean:
	rm -rf $(CLASSPATH)

# Purge all generated files, including jars
purge: clean
	rm -rf $(BINDIR)/*.jar
	rm -rf $(LOGDIR)/*.log*

# Create client jar file
$(CLIENT_JAR): classfiles | $(BINDIR)
	jar cfm $(CLIENT_JAR) $(RESOURCES)/MANIFEST.MF -C $(CLASSPATH) .

# Run the client interactively (no file, requires number-of-replicas)
run_interactive: $(CLIENT_JAR)
	java -cp $(CLIENT_JAR):$(DEPS) $(MAIN_CLASS) $(SPREAD_SERVER_ADDRESS) $(ACCOUNT_NAME) $(REPLICAS)

# Run the client in simulation mode (with file name, requires no number-of-replicas)
run_simulation: $(CLIENT_JAR)
	java -cp $(CLIENT_JAR):$(DEPS) $(MAIN_CLASS) $(SPREAD_SERVER_ADDRESS) $(ACCOUNT_NAME) 3 $(COMMAND_FILE) 

# Run the client for a single replica (no number-of-replicas argument)
run_single_replica: $(CLIENT_JAR)
	java -cp $(CLIENT_JAR):$(DEPS) $(MAIN_CLASS) $(SPREAD_SERVER_ADDRESS) $(ACCOUNT_NAME) 1 $(COMMAND_FILE)

# Build all
all: classfiles $(CLIENT_JAR)

.PHONY: all clean purge run_interactive run_simulation run_single_replica
.DEFAULT_GOAL := classfiles

