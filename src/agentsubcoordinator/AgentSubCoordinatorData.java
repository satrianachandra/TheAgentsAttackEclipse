/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentsubcoordinator;

import jade.core.AID;

/**
 *
 * @author Ethan_Hunt
 */
public class AgentSubCoordinatorData {
    private String machineIP;
    private AID myAID;
    
    public AgentSubCoordinatorData(String machineIP, AID myAID){
        this.machineIP = machineIP;
        this.myAID = myAID;
    }
    
    public String getMachineIP(){
        return this.machineIP;
    }
    
    public AID getAID(){
        return this.myAID;
    }
    
}
