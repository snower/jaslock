package com.github.snower.slock.commands;

public interface ICommand {
    public final static byte MAGIC = 0x56;
    public final static byte VERSION = 0x01;
    public final static byte COMMAND_TYPE_LOCK = 0x01;
    public final static byte COMMAND_TYPE_UNLOCK = 0x02;

    public final static byte COMMAND_RESULT_SUCCED = 0x00;
    public final static byte COMMAND_RESULT_UNKNOWN_MAGIC = 0x01;
    public final static byte COMMAND_RESULT_UNKNOWN_VERSION = 0x02;
    public final static byte COMMAND_RESULT_UNKNOWN_DB = 0x03;
    public final static byte COMMAND_RESULT_UNKNOWN_COMMAND = 0x04;
    public final static byte COMMAND_RESULT_LOCKED_ERROR = 0x05;
    public final static byte COMMAND_RESULT_UNLOCK_ERROR = 0x06;
    public final static byte COMMAND_RESULT_UNOWN_ERROR = 0x07;
    public final static byte COMMAND_RESULT_TIMEOUT = 0x08;
    public final static byte COMMAND_RESULT_EXPRIED = 0x09;
    public final static byte COMMAND_RESULT_STATE_ERROR = 0x0a;
    public final static byte COMMAND_RESULT_ERROR = 0x0b;

    public final static byte LOCK_FLAG_SHOW_WHEN_LOCKED   = 0x01;
    public final static byte LOCK_FLAG_UPDATE_WHEN_LOCKED = 0x02;
    public final static byte LOCK_FLAG_FROM_AOF           = 0x04;

    int getCommandType();
    byte[] getRequestId();
    ICommand parseCommand(byte[] buf);
    byte[] dumpCommand();
}
