JFLAGS = -g
JC = javac
JVM = java -jar
RM = rm

SRC = tcp/TCPend.java \
			tcp/Receiver.java \
			tcp/Client.java \
			tcp/Server.java \
			tcp/ServerReader.java \
			tcp/ClientWriter.java \
			tcp/TCPv2.java \
			tcp/TimeOut.java 

MAINCLASS = TCPend

CLASSES = $(SRC:.java=.class)

$(CLASSES): %.class: %.java
	$(JC) $(JFLAGS) $<

clean:
	$(RM) tcp/*.class
