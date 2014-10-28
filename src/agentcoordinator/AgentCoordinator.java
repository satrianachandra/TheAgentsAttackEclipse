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
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.gui.GuiAgent;
import jade.gui.GuiEvent;
import jade.lang.acl.ACLMessage;
import jade.core.Runtime;
import jade.lang.acl.UnreadableException;
import jade.tools.logging.ontology.GetAllLoggers;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;

import messageclasses.SmithParameter;
import utils.MyLogger;
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
    private final double max_agents_per_machine = 2500.0;
    private SmithParameter pendingSP;
    
    private int numberOfInstanceRequired=1;
    private boolean isWaitingForInstance=false;
    private List<String>instanceIDList;
    
    public static final String SEMICOLON = ";";
    //an example of adding 1 remote platforms
    public AID remoteDF;
    private AgentCoordinatorUI agentUI;
    public static List<AID>listOfSubCoordinators;
    private int numberOfRunningAgents=0;
    public static List<Process>sshProcessess;
    private AmazonEC2Client amazonEC2Client;
    
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
        
         
        //keep list of the SCs
        listOfSubCoordinators = new ArrayList<>();
        
        //instance IDs list
        instanceIDList = new ArrayList<>();
        
        //AWS SDK stuffs
        try {
			AWSCredentials credentials =
					  new PropertiesCredentials(
					         AgentCoordinator.class.getResourceAsStream("AwsCredentials.properties"));
			amazonEC2Client =
	        		  new AmazonEC2Client(credentials);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        amazonEC2Client.setEndpoint("ec2.eu-west-1.amazonaws.com");
        
        
        
        
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
        
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
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
                            numberOfRunningAgents= sp.numberOfRunningAgents + numberOfRunningAgents;
                            agentUI.updateNumberOfAgents(numberOfRunningAgents);
                        }if (Message_Performative.equals("INFORM")&& sp.type==MESSAGE_I_AM_UP){
                            //launchAgentsInSC(sender);
                        	System.out.println("An instance is up!!");
                        	System.out.println(sender.getName()+" is up");
                        	agentUI.setStatus("Ready");
                        	if (!listOfSubCoordinators.contains(sender)){
                        		listOfSubCoordinators.add(sender);
                        		if ((listOfSubCoordinators.size()>=numberOfInstanceRequired) && (isWaitingForInstance)){
                        			launchAllAgents(pendingSP);
                        			agentUI.setTextAreaContent("Instances ready, launching agents...");
                        			agentUI.appendTextAreaContent("\nAdditional Instances:\n");
                        			for (int i=0;i<instanceIDList.size();i++){
                                        agentUI.appendTextAreaContent(instanceIDList.get(i));
                                    }
                        		}else if (isWaitingForInstance){
                        			agentUI.setStatus("launching additional instances, please wait ...");
                        		}
                        	}
                        }
                    } catch (UnreadableException ex) {
                        Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
            }

        } 
    }

    
    public static void main(String[]args){
    	/*
    	try {
			MyLogger.setup();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	MyLogger.setLevel(3);
    	*/
    	
    	// Get a hold on JADE runtime
        Runtime rt = Runtime.instance();
        // Exit the JVM when there are no more containers around
        rt.setCloseVM(true);
        System.out.print("runtime created\n");
        
        
        //start main container
        ProfileImpl mProfile = new ProfileImpl(null,1099,null);
        mProfile.setParameter("jade_domain_df_maxresult", "4000");
        jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(mProfile);
        /*
        
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
      
        
    }
    
    private void killAllAgentSmith(){
        //send a kill message to the AgentSubCoordinator  
        sendMessageToSC(MESSAGE_KILL_AGENTS);
    }
        
    private void getNumberOfAgents(){
        //numberOfRunningAgents=0;
        //sendMessageToSC(GET_NUMBER_OF_AGENTS);
    	int numberofrunningagents = 0;
    	System.out.println("size: "+listOfSubCoordinators.size());
    	for (int i=0;i<listOfSubCoordinators.size();i++){
    		String remoteDfName = "df@"+listOfSubCoordinators.get(i).getName().split("@")[1];
    		System.out.println(remoteDfName);
    		AID dfAID = new AID(remoteDfName, AID.ISGUID);
    		dfAID.addAddresses(listOfSubCoordinators.get(i).getAddressesArray()[0]);
    		try {
				numberofrunningagents = numberofrunningagents+ getNumberOfAgents(dfAID);
			} catch (FIPAException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	agentUI.updateNumberOfAgents(numberofrunningagents);
    	
    }
    
    private void sendMessageToSC(int spType){
    	SmithParameter sp = new SmithParameter();
        sp.type=spType; 
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.setLanguage("English");
        try {
            msg.setContentObject(sp);
        } catch (IOException ex) {
            Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
        }
    	for (int i=0;i<listOfSubCoordinators.size();i++){
            msg.addReceiver(listOfSubCoordinators.get(i));
        }
    	System.out.println("List of receivers: "+msg.getAllIntendedReceiver().toString());
    	send(msg);
        
    }

    private void launchAllAgents(SmithParameter sp){
        //send message to the subcoordinator to launch agent
        
    	
    	numberOfInstanceRequired = (int) Math.ceil(sp.numberOfAgent/max_agents_per_machine);
    	System.out.println("instance required: "+numberOfInstanceRequired);
    	
    	int additionalInstance = numberOfInstanceRequired - listOfSubCoordinators.size();
    	System.out.println("additional instances: "+additionalInstance);
    	if (additionalInstance>0){
    		//launch additional instance
    		launchAdditionalInstances(additionalInstance);
    		//this will block, and then need to wait for the jade and SC run on the machine.
    		isWaitingForInstance = true;
    		pendingSP = sp;
    		//show loading screen
    		agentUI.setTextAreaContent("launching "+additionalInstance+" additional instances, please wait...");
    	}else{
    		//dividing the agents across machines
    		int agentsPerMachine = sp.numberOfAgent/(listOfSubCoordinators.size());
    		System.out.println("agents permachine: "+agentsPerMachine);
    		int leftOvers = sp.numberOfAgent % listOfSubCoordinators.size(); 
	        sp.numberOfAgent = agentsPerMachine;
	        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
	        msg.setLanguage("English");
            try {
                msg.setContentObject(sp);
            } catch (IOException ex) {
                Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
            }
	        for (int i=0;i<listOfSubCoordinators.size();i++){
	            msg.addReceiver(listOfSubCoordinators.get(i));
	        }
	        System.out.println("List of receivers: "+msg.getAllIntendedReceiver().toString());
            send(msg);
            
            if (leftOvers>0){
	            sp.numberOfAgent = leftOvers;
	            ACLMessage msg2 = new ACLMessage(ACLMessage.REQUEST);
	            msg2.addReceiver(listOfSubCoordinators.get(listOfSubCoordinators.size()-1));
		        msg2.setLanguage("English");
	            try {
	                msg2.setContentObject(sp);
	            } catch (IOException ex) {
	                Logger.getLogger(AgentCoordinator.class.getName()).log(Level.SEVERE, null, ex);
	            }
	            send(msg2);
            }
            
    	}
    }

    
    private void launchAdditionalInstances(int numberOfInstance){
    	RunInstancesRequest runInstancesRequest = 
    			  new RunInstancesRequest();
    		        	
    		  runInstancesRequest.withImageId("ami-84c36bf3") 
    		                     .withInstanceType("t2.micro")
    		                     .withMinCount(numberOfInstance)
    		                     .withMaxCount(numberOfInstance)
    		                     .withSubnetId("subnet-a020fac5")
    		                     .withKeyName("14 _LP1_KEY_ D7001D_CHASAT-4")
    		                     .withSecurityGroupIds("sg-cbc665ae").withMonitoring(true);
    		  RunInstancesResult runInstancesResult = 
    				  amazonEC2Client.runInstances(runInstancesRequest);
    		  
    		  Reservation myReservation = runInstancesResult.getReservation();
    		  List<Instance> instanceList=  myReservation.getInstances();
    		  for (int i=0;i<instanceList.size();i++){
    			  Instance instance= instanceList.get(i);
    			  instanceIDList.add(instance.getInstanceId()); //add to our list of instances ID
    			  //then tag them for easy finding
    			  CreateTagsRequest createTagsRequest = new CreateTagsRequest();
    			  createTagsRequest.withResources(instance.getInstanceId()).withTags(new Tag("Name", "SCAgent-TeamAsia-" + i));
    			  amazonEC2Client.createTags(createTagsRequest);
    		  }
    		  
    }

    private void terminateInstance(String instanceID){
    	TerminateInstancesRequest terminateInstanceRequest =  new TerminateInstancesRequest().withInstanceIds(instanceID);
    	TerminateInstancesResult terminateInstanceResult = amazonEC2Client.terminateInstances(terminateInstanceRequest);
    }
    
    public void terminateAllAdditionalInstances(){
    	agentUI.appendTextAreaContent("terminating additional instances");
    	for (int i=0;i<instanceIDList.size();i++){
    		terminateInstance(instanceIDList.get(i));
    	}
    }
    
    @Override
    protected void takeDown(){
        try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private int getNumberOfAgents(AID dfAID) throws FIPAException{
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription templateSd = new ServiceDescription();
        templateSd.setType("AgentSmith");
        template.addServices(templateSd);
        SearchConstraints sc = new SearchConstraints();
        // We want to receive 10 results at most
        sc.setMaxResults(new Long(-1));
        sc.setMaxDepth(1L);
        DFAgentDescription[] results = DFService.search(this,dfAID, template, sc);
        
        /*
        List<AID> myAgentsList = new ArrayList<AID>();
        if (results.length>0){
            for(int i=0;i<results.length;i++){
                DFAgentDescription agentDesc = results[i];
                AID provider = agentDesc.getName();
                myAgentsList.add(provider);
           }   
        }
        */
        return results.length;
    }


    
}

