/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentsmith;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import agentsubcoordinator.AgentSubCoordinator;

/**
 *
 * @author Ethan_Hunt
 */
public class AgentSmith extends Agent {
    
    private long interval;
    
    private String serverAddress;
    private int serverPort;
    private AgentSmith theAgent;
    private String fiboNumber;
    //private AID coordinatorAID;
    
    private final Logger LOGGER =
            Logger.getLogger(AgentSmith.class.getName());
    private Socket tcpClientSocket;
    
    //private PrintWriter out ;
    //private BufferedReader in ;
            
    private boolean mustBeKilled = false;
    
    @Override
    protected void setup() {
        theAgent = this;
        AgentSubCoordinator.smithList.add(this);
        
        LOGGER.log(Level.INFO, "Agent "+getAID().getName()+"is up");
        
        Object[] args = getArguments();
        if (args != null){
            interval = (long) args[0];
            serverAddress = (String)args[1];
            serverPort = (int)args[2];
            fiboNumber = (String)args[3];
            //coordinatorAID = (AID)args[3];
            /*
            interval = Long.decode(args[0].toString());
            serverAddress = args[1].toString();
            serverPort = Integer.decode(args[2].toString());
            coordinatorAID = new AID(args[3].toString(), AID.ISGUID);
            coordinatorAID.addAddresses(args[4].toString());
            */
        }
        
        /*
        //registration to the DF, so we can search the agents later, need to check if necessary
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("AgentSmith");
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
        */
        TickerBehaviour tb = new TickerBehaviour(this, interval) {
            protected void onTick() {
                if (mustBeKilled){
                    System.out.println("Hej Hej");
                    theAgent.removeBehaviour(this);
                    doDelete();
                    
                }
                try{
                //if ((tcpClientSocket == null)||tcpClientSocket.isClosed()){
                tcpClientSocket = new Socket(serverAddress, serverPort);
                PrintWriter out = new PrintWriter(tcpClientSocket.getOutputStream(), true);
                //BufferedReader in = new BufferedReader(
                //new InputStreamReader(tcpClientSocket.getInputStream()));
                out.println(fiboNumber);
                //out = new PrintWriter(tcpClientSocket.getOutputStream(), true);
                //in = new BufferedReader(new InputStreamReader(tcpClientSocket.getInputStream()));
                
                //String result="";
                //result = in.readLine();                //out.close();
                //inform the Coordinator
                //informCoordinator("fibo result: "+result);
                //in.close();
                
                
                }catch(UnknownHostException e){
                    System.err.println("Don't know about host " + serverAddress);
                    //System.exit(1);
                } catch (IOException ex) {
                    Logger.getLogger(AgentSmith.class.getName()).log(Level.SEVERE, null, ex);
                    //inform the Coordinator
                    //informCoordinator("Failed opening TCP socket to server");
                
                    //System.exit(1);
                }catch(Exception ex){
                    Logger.getLogger(AgentSmith.class.getName()).log(Level.SEVERE, null, "failed due to time out or refused");
                }finally{
                    
                    try {
                        if (tcpClientSocket!=null){ 
                        	tcpClientSocket.close();
                        	System.out.println("AID:"+getAID().getName());
                        	LOGGER.log(Level.INFO, "AID:{0}",getAID().getName());
                        }
                        
                    } catch (IOException ex) {
                        Logger.getLogger(AgentSmith.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                
            }
        };
        addBehaviour(tb);
        
       // addBehaviour(new ReceiveMessage());
    }
    
    @Override
    protected void takeDown(){
        /*
        try {
            if (in !=null){
                in.close();
                out.close();
                tcpClientSocket.close();    
            }
        } catch (IOException ex) {
            Logger.getLogger(AgentSmith.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
    }
    
    /*
    public class SendMessage extends OneShotBehaviour {
    
    private ACLMessage msg;
    
    public SendMessage(ACLMessage msg){
        super();
        this.msg = msg;
    }

    @Override
    public void action() {
        myAgent.send(msg);
        System.out.println("****I Sent Message to::>  *****"+"\n"+
                            "The Content of My Message is::>"+ msg.getContent());

        }
    }
    */

    /*
    public class ReceiveMessage extends CyclicBehaviour {
   // Variable to Hold the content of the received Message
    private String Message_Performative;
    private String Message_Content;
    private String SenderName;
    private String MyPlan;
    
    public void action() {
        ACLMessage msg = receive();
        if(msg != null) {
            
            Message_Performative = msg.getPerformative(msg.getPerformative());
            Message_Content = msg.getContent();
            SenderName = msg.getSender().getLocalName();
            System.out.println(" ****I Received a Message***" +"\n"+
                    "The Sender Name is::>"+ SenderName+"\n"+
                    "The Content of the Message is::> " + Message_Content + "\n"+
                    "::: And Performative is::> " + Message_Performative + "\n");
            System.out.println("ooooooooooooooooooooooooooooooooooooooo");
        
        }

    } 
    
    }
    */

    public void killThisAgent(){
        mustBeKilled=true;
    }
    
}
