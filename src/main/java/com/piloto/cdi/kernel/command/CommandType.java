package com.piloto.cdi.kernel.command;

/**
 * Enumeration of command types that can be issued to the system.
 * 
 * <p>Each command type represents a distinct operation category that the
 * Executive Controller can process. Commands are validated against policies
 * and translated into domain events.</p>
 * 
 * <p><b>Phase 3 Supported Types:</b></p>
 * <ul>
 *   <li>CREATE_GOAL - Initiate a new goal</li>
 *   <li>UPDATE_GOAL - Modify existing goal parameters</li>
 *   <li>COMPLETE_GOAL - Mark goal as successfully completed</li>
 *   <li>UPDATE_STATE - General state property update</li>
 *   <li>SYSTEM_COMMAND - Administrative operations</li>
 * </ul>
 * 
 * @since Phase 3
 */
public enum CommandType {
    CREATE_GOAL,
    UPDATE_GOAL,
    COMPLETE_GOAL,
    UPDATE_STATE,
    SYSTEM_COMMAND
}
