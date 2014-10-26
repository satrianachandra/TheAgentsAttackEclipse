#!/bin/sh
javac -cp "/home/ubuntu/Codes/TheAgentsAttackEclipse/lib/*" /home/ubuntu/Codes/TheAgentsAttackEclipse/src/*/*.java
cd /home/ubuntu/Codes/TheAgentsAttackEclipse/src/&&java -cp "/home/ubuntu/Codes/TheAgentsAttackEclipse/lib/*" agentcoordinator.AgentCoordinator