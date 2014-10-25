/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentcoordinator;

import agentsubcoordinator.AgentSubCoordinatorData;
import jade.core.AID;
import jade.core.ProfileImpl;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.core.Runtime;
import jade.lang.acl.UnreadableException;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import messageclasses.SmithParameter;
import utils.Terminal;



/**
 *
 * @author Ethan_Hunt
 */
public class AgentCoordinator extends GuiAgent {

    public static final int MESSAGE_RECEIVED = 1;
    public static final int MESSAGE_SENT = 2;
    public static final int MESSAGE_LAUNCH_AGENTS = 3;
    public static final int MESSAGE_KILL_AGENTS = 4;
    public static final int GET_NUMBER_OF_AGENTS = 5;
    public static final int MESSAGE_I_AM_UP = 6;
    
    public static final String SEMICOLON = ";";
    //an example of adding 1 remote platforms
    public AID remoteDF;
    private AgentCoordinatorUI agentUI;
    public static List<AgentSubCoordinatorData>listRemoteSubCoordinators;
    private int numberOfRunningAgents=0;
    public static List<Process>sshProcessess;
    
    protected void setup() {
        /** Registration with the DF */
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("AgentCoordinator");
        sd.setName(getName());
        sd.setOwnership("JADE");
        sd.addOntologies("JADEAgent");
        dfd.setName(getAID());

        dfd.addServices(sd);
        try {
        DFService.register(this,dfd);
        } catch (FIPAException e) {
        System.err.println(getLocalName()+" registration with DF unsucceeded. Reason: "+e.getMessage());
        //doDelete();
        }
        /*
        AID aDF = new AID("df@Platform2",AID.ISGUID);
        aDF.addAddresses("http://sakuragi:54960/acc");
        */
        agentUI = new AgentCoordinatorUI();
        agentUI.setAgent(this);
        agentUI.setTitle("Coordinator Agent " + this.getName());
        //try {
            //RefetchAgentsList();
            //RA1.populateAgentsListOnGUI();
        //} catch (FIPAException ex) {
            //Logger.getLogger(AgentCommGUI.class.getName()).log(Level.SEVERE, null, ex);
        //}
        ReceiveMessage rm = new ReceiveMessage();
        addBehaviour(rm);
    }
    
    
    
    @Override
    protected void onGuiEvent(GuiEvent ge) {
        int type = ge.getType();
        if (type == MESSAGE_LAUNCH_AGENTS){
            SmithParameter sp = (SmithParameter)ge.getParameter(0);
            launchAllAgents(sp);
        }else if (type == MESSAGE_KILL_AGENTS){
            killAllAgentSmith();
        }else if (type == GET_NUMBER_OF_AGENTS){
            getNumberOfAgents();
        }
    }
    
    public class SendMessage extends OneShotBehaviour {
        
        private ACLMessage msg;
        
        public SendMessage(ACLMessage msg){
            super();
            this.msg = msg;
        }
        @Override
        public void action() {
            send(msg);
            System.out.println("****I Sent Message to::> R1 *****"+"\n"+
                                "The Content of My Message is::>"+ msg.getContent());
            
            //GuiEvent ge = new GuiEvent(this, AgentCommGUI.MESSAGE_SENT);
            //ge.addParameter(msg);
            //postGuiEvent(ge);
        }
    }

    public class ReceiveMessage extends CyclicBehaviour {
   // Variable to Hold the content of the received Message
        private String Message_Performative;
        private String Message_Content;
        private String SenderName;
        private String MyPlan;
        private AID sender;
        
        public void action() {
            ACLMessage msg = receive();
            if(msg != null) {
                Message_Performative = msg.getPerformative(msg.getPerformative());
                Message_Content = msg.getContent();
                sender = msg.getSender();
                SenderName = msg.getSender().getLocalName();
                System.out.println(" ****I Received a Message***" +"\n"+
                        "The Sender Name is::>"+ SenderName+"\n"+
                        "The Content of the Message is::> " + Message_Content + "\n"+
                        "::: And Performative is::> " + Message_Performative + "\n");
                System.out.println("ooooooooooooooooooooooooooooooooooooooo");
                
                if (msg.hasByteSequenceContent()){
                    SmithParameter sp = null;
                    try {
                        sp = (SmithParameter)msg.getContentObject();
                        if (Message_Performative.equals("INFORM")&& sp.type==GET_NUMBER_OF_AGENTS){
                            numberOfRunningAgents+=sp.numberOfRunningAgents;
                            agentUI.updateNumberOfAgents(numberOfRunningAgents);
                        }if (Message_Performative.equals("INFORM")&& sp.type==MESSAGE_I_AM_UP){
                            launchAgentsInSC(sender);
                        }
                    } catch (UnreadableException ex) {
                        Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
            }

        } 
    }

    
    public static void main(String[]args){
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
        System.out.print("runtime created\n");
        
        /*
        //start main container
        ProfileImpl mProfile = new ProfileImpl(null,1099,null);
        jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(mProfile);
        
        
        //starting RMA agent for monitoring purposes
        try {
            AgentController agentRMA = mainContainer.createNewAgent("RMA","jade.tools.rma.rma", null);
            agentRMA.start();
        } catch (StaleProxyException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
        
        //creating container for other agents
        ProfileImpl pContainer = new ProfileImpl();//null, startingPort+i,null);
        jade.wrapper.AgentContainer agentContainer = rt.createAgentContainer(pContainer);
        
        //Start the Agent Coordinator
        AgentController agentCoordinator;
        Object[] coordinatorArgs = new Object[1];
        coordinatorArgs[0]="0";
        try {
            agentCoordinator = agentContainer.createNewAgent("TheCoordinator",
                    "agentcoordinator.AgentCoordinator", coordinatorArgs);
            agentCoordinator.start();
        } catch (StaleProxyException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //Start the local Agent SubCoordinator
        AgentController agentSubCoodinator;
        Object[] subCoordArgs = new Object[1];
        try {
            agentSubCoodinator = agentContainer.createNewAgent("SC",
                    "agentsubcoordinator.AgentSubCoordinator", subCoordArgs);
            agentSubCoodinator.start();
        } catch (StaleProxyException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
        //Add remote machines
        listRemoteSubCoordinators = new ArrayList<>();
        
        AID remoteSubCoordinator = new AID("SC@172.30.1.158:1099/JADE", AID.ISGUID);
        remoteSubCoordinator.addAddresses("http://ip-172-30-1-158.eu-west-1.compute.internal:7778/acc");
        //listRemoteSubCoordinators.add(new AgentSubCoordinatorData("172.30.1.158", remoteSubCoordinator));
        
        remoteSubCoordinator = new AID("SC@172.30.1.232:1099/JADE", AID.ISGUID);
        remoteSubCoordinator.addAddresses("http://ip-172-30-1-232.eu-west-1.compute.internal:7778/acc");
        //listRemoteSubCoordinators.add(new AgentSubCoordinatorData("172.30.1.232", remoteSubCoordinator));
        
        System.out.print("list size: "+listRemoteSubCoordinators.size());
        //start the agents in the remotes
        sshProcessess = new ArrayList<>();
        for(int i=0;i<listRemoteSubCoordinators.size();i++){
            String hostname = listRemoteSubCoordinators.get(i).getMachineIP();
            String setClasspath = "export CLASSPATH=:$CLASSPATH;";
            String createPlatformAndAgents= " cd /home/ubuntu/Codes/TheAgentsAttack/src&&java agentsubcoordinator.AgentSubCoordinator;";
            Process pr = Terminal.executeNoError("ssh -X -o StrictHostKeyChecking=no -i /home/ubuntu/aws_key_chasat.pem "+hostname+" "+"\""+setClasspath+createPlatformAndAgents+"\"");
            sshProcessess.add(pr);
        }
           
    }
    
    private void killAllAgentSmith(){
        //send a kill message to the AgentSubCoordinator  
        sendMessageToSmith(MESSAGE_KILL_AGENTS);
    }
        
    private void getNumberOfAgents(){
        numberOfRunningAgents=0;
        sendMessageToSmith(GET_NUMBER_OF_AGENTS);
    }
    
    private void launchAgentsInSC(AID scAID){
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(scAID);
        msg.setLanguage("English");
        try {
            SmithParameter sp = new SmithParameter();
            sp.numberOfAgent=1000;
            sp.serverAddress = "172.30.1.98";
            sp.serverPort=8080;
            sp.fiboNumber="1000";
            sp.interval=1000L;
            sp.type=MESSAGE_LAUNCH_AGENTS; 
            msg.setContentObject(sp);
        } catch (IOException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        send(msg);

    }
    
    private void sendMessageToSmith(int spType){
        //the local
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("SC", AID.ISLOCALNAME));
        msg.setLanguage("English");
        try {
            SmithParameter sp = new SmithParameter();
            sp.type=spType; 
            msg.setContentObject(sp);
        } catch (IOException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        //msg.setContent("launch agents");
        send(msg);
        
        //the remotes
        for (int i=0;i<listRemoteSubCoordinators.size();i++){
            SmithParameter sp = new SmithParameter();
            sp.type=spType; 
            ACLMessage msg2 = new ACLMessage(ACLMessage.REQUEST);
            msg2.setLanguage("English");
            try {
                msg2.setContentObject(sp);
            } catch (IOException ex) {
                Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            msg2.addReceiver(listRemoteSubCoordinators.get(i).getAID());
            System.out.println("List of receivers: "+msg.getAllIntendedReceiver().toString());
            send(msg2);
            
        }
        
        
    }

    private void launchAllAgents(SmithParameter sp){
        //send message to the subcoordinator to launch agent
        
        //dividing the agents across machines
        int agentsPerMachine = sp.numberOfAgent/(listRemoteSubCoordinators.size()+1);
        System.out.println("number of agents: "+agentsPerMachine);
        
        sp.numberOfAgent = agentsPerMachine;
        System.out.println(sp.numberOfAgent);
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        
        //for local SC agent
        msg.addReceiver(new AID("SC", AID.ISLOCALNAME));
        msg.setLanguage("English");
        try {
            msg.setContentObject(sp);
        } catch (IOException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        send(msg);

        //for remote SC agents
        for (int i=0;i<listRemoteSubCoordinators.size();i++){
            ACLMessage msg2 = new ACLMessage(ACLMessage.REQUEST);
            msg2.setLanguage("English");
            try {
                msg2.setContentObject(sp);
            } catch (IOException ex) {
                Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            msg2.addReceiver(listRemoteSubCoordinators.get(i).getAID());
            System.out.println("List of receivers: "+msg.getAllIntendedReceiver().toString());
            send(msg2);
        }   
    }

    
    @Override
    protected void takeDown(){
        for (int i=0;i<sshProcessess.size();i++){
            sshProcessess.get(i).destroy();
        }
    }
}

