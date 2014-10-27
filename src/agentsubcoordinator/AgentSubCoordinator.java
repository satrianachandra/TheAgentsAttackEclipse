/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentsubcoordinator;

import agentcoordinator.AgentCoordinator;
import agentsmith.AgentSmith;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ProfileImpl;
import jade.wrapper.AgentController;

import java.util.List;

import jade.core.Runtime;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.UnreadableException;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import utils.MyLogger;
import messageclasses.SmithParameter;
/**
 *
 * @author Ethan_Hunt
 */
public class AgentSubCoordinator extends Agent {
    
    private List<jade.wrapper.AgentContainer> mainContainersList;
    private List<AgentController> agentsList;
    private int platformNumber=0;
    private int numberOfRunningAgents = 0;
    public static jade.wrapper.ContainerController agentContainer;
    //private jade.wrapper.ContainerController agentSmithContainer;
    private AID coordinatorAID;
    
    public static List<AgentSmith>smithList = new ArrayList<>();
    
    @Override
    protected void setup() {
        //registration to the DF, so we can search the agents later, need to check if necessary
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("AgentSubCoordinator");
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
        //hard coding the Coordinator's AID, will be CHANGED, ok!
        //coordinatorAID = new AID("TheCoordinator@172.30.1.7:1099/JADE", AID.ISGUID);
      //coordinatorAID.addAddresses("http://ip-172-30-1-7.eu-west-1.compute.internal:7778/acc");
        coordinatorAID = new AID("TheCoordinator@172.30.1.217:1099/JADE", AID.ISGUID);
        coordinatorAID.addAddresses("http://ip-172-30-1-217.eu-west-1.compute.internal:7778/acc");
        
        
        ReceiveMessage rm = new ReceiveMessage();
        addBehaviour(rm);
        
        notifyCoordinator();
    }
    
    //@Override
    //protected void takeDown(){
        
    //}
    
    private void startAgentSmiths(int numberOfAgents, long interval, String serverAddress, int serverPort,String fiboNumber ){
        
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
        System.out.print("runtime created\n");
        
        mainContainersList = new ArrayList<>();
        agentsList = new ArrayList<>();
        //listOfProcesses = new ArrayList<>();
        AgentController agentSmith;
        //Profile mProfile = new ProfileImpl("192.168.0.102", startingPort+i,"Platform-"+i+":"+(startingPort+i),false);
        //Profile mProfile = new ProfileImpl("192.168.0.102", startingPort+i,null);
        //jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(mProfile);
        //System.out.println("main container created "+mainContainer);
        //mainContainersList.add(mainContainer);

        ProfileImpl pContainer = new ProfileImpl();//null, startingPort+i,null);
        jade.wrapper.ContainerController agentSmithContainer = rt.createAgentContainer(pContainer);
        System.out.println("containers created "+pContainer);
        for (int j=0;j<numberOfAgents;j++){
            try {
                Object[] smithArgs = new Object[4];
                smithArgs[0] = interval;
                smithArgs[1] = serverAddress;
                smithArgs[2] = serverPort;
                smithArgs[3] = fiboNumber;
                //smithArgs[3] = getAID(); //the subcoordinator's aid
                agentSmith = agentSmithContainer.createNewAgent("Platform-"+platformNumber+"_Smith-"+j,
                        "agentsmith.AgentSmith", smithArgs);
                //agentSmith = agentSmithContainer.createNewAgent("Smith-"+j,
                //               "agentsmith.AgentSmith", smithArgs);
                agentSmith.start();
                numberOfRunningAgents++;
                agentsList.add(agentSmith);
            } catch (StaleProxyException ex) {
                Logger.getLogger(AgentSubCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    
    /*
    private void killAllAgentSmith() throws StaleProxyException{
        for (int i=0;i<mainContainersList.size();i++){
            mainContainersList.get(i).kill();
        }
        
    }*/
    
    private List<AID> findAgents(AID dfAID) throws FIPAException{
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType("AgentSmith");
        template.addServices(templateSd);
        SearchConstraints sc = new SearchConstraints();
        // We want to receive 10 results at most
        //sc.setMaxResults(new Long(20));
        sc.setMaxDepth(1L);
        DFAgentDescription[] results = DFService.search(this,dfAID, template, sc);
        
        List<AID> myAgentsList = new ArrayList<AID>();
        if (results.length>0){
            for(int i=0;i<results.length;i++){
                DFAgentDescription agentDesc = results[i];
                AID provider = agentDesc.getName();
                myAgentsList.add(provider);
           }   
        }
        return myAgentsList;
    }

    
    public class SendMessage extends OneShotBehaviour {
        
        private ACLMessage msg;
        
        private AgentSubCoordinator myAgent;
        
        public SendMessage(ACLMessage msg,AgentSubCoordinator myAgent){
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
            //myAgent.postGuiEvent(ge);
        }
    }
    
    public class ReceiveMessage extends CyclicBehaviour {
   // Variable to Hold the content of the received Message
    private String Message_Performative;
    private String Message_Content;
    private String SenderName;
    private String MyPlan;
    private SmithParameter sp;
    
    public void action() {
        ACLMessage msg = receive();
        
        if(msg != null) {

            Message_Performative = msg.getPerformative(msg.getPerformative());
            Message_Content = msg.getContent();
            SenderName = msg.getSender().getLocalName();
            
            if (msg.hasByteSequenceContent()){
            try {
                
                if (msg.getContentObject()!=null){
                    sp = (SmithParameter)msg.getContentObject();
                    if (Message_Performative.equals("REQUEST") && sp.type==AgentCoordinator.MESSAGE_LAUNCH_AGENTS && sp.numberOfAgent>0 ){
                        startAgentSmiths(sp.numberOfAgent,sp.interval, sp.serverAddress, sp.serverPort,sp.fiboNumber );
                    }else if (Message_Performative.equals("REQUEST") && sp.type==AgentCoordinator.MESSAGE_KILL_AGENTS){
                        killAgents();
                    }else if (Message_Performative.equals("REQUEST") && sp.type==AgentCoordinator.GET_NUMBER_OF_AGENTS){
                        //send back the number of running agents
                        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
                        response.addReceiver(coordinatorAID);
                        response.setLanguage("English");
                        try {
                            SmithParameter sp = new SmithParameter();
                            sp.type=AgentCoordinator.GET_NUMBER_OF_AGENTS;
                            sp.numberOfRunningAgents = numberOfRunningAgents;
                            response.setContentObject(sp);
                        } catch (IOException ex) {
                            Logger.getLogger(AgentSubCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        send(response);
                        
                    }
                }
            } catch (UnreadableException ex) {
                Logger.getLogger(AgentSubCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
            }
            
            
            System.out.println(" ****I Received a Message***" +"\n"+
                    "The Sender Name is::>"+ SenderName+"\n"+
                    //"The Content of the Message is::> " + sp.toString() + "\n"+
                    "::: And Performative is::> " + Message_Performative + "\n");
            System.out.println("ooooooooooooooooooooooooooooooooooooooo");
           
           

        }

    }
    
    
    }
    
    public static void main(String[]args){
    	try {
			MyLogger.setup();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	MyLogger.setLevel(3);
    	
        // Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
        System.out.print("runtime created\n");
        
        
        ProfileImpl mProfile = new ProfileImpl();
        jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(mProfile);
        
        
        //start the RMA agent
        //starting RMA agent for monitoring purposes
        /*
        try {
            AgentController agentRMA = mainContainer.createNewAgent("RMA","jade.tools.rma.rma", null);
            agentRMA.start();
        } catch (StaleProxyException ex) {
            Logger.getLogger(AgentSubCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        */

        
        ProfileImpl pContainer = new ProfileImpl();//null, startingPort+i,null);
        AgentSubCoordinator.agentContainer = rt.createAgentContainer(pContainer);        
        
        //Start the local Agent SubCoordinator
        AgentController agentSubCoodinator;
        Object[] subCoordArgs = new Object[1];
        try {
            agentSubCoodinator = AgentSubCoordinator.agentContainer.createNewAgent("SC",
                    "agentsubcoordinator.AgentSubCoordinator", subCoordArgs);
            agentSubCoodinator.start();
        } catch (StaleProxyException ex) {
            Logger.getLogger(AgentSubCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void killAgents(){
        System.out.println("killing agents");
        if(smithList.size()!=0){
            for (int i=0;i<smithList.size();i++){
                smithList.get(i).killThisAgent();
                smithList.remove(i);
                killAgents();
            }
        }
    }
    
    
    @Override
    protected void takeDown(){
        killAgents();
    }
    

    public void notifyCoordinator(){
        //the local
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.addReceiver(coordinatorAID);
        msg.setLanguage("English");
        try {
            SmithParameter sp = new SmithParameter();
            sp.type=AgentCoordinator.MESSAGE_I_AM_UP; 
            msg.setContentObject(sp);
        } catch (IOException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
        //msg.setContent("launch agents");
        send(msg);

    }
}
