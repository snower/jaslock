package com.github.snower.slock.commands;

public interface ICommand {
    public final static byte MAGIC = 0x56;
    public final static byte VERSION = 0x01;
    public final static byte COMMAND_TYPE_INIT = 0x00;
    public final static byte COMMAND_TYPE_LOCK = 0x01;
    public final static byte COMMAND_TYPE_UNLOCK = 0x02;
    public final static byte COMMAND_TYPE_PING = 0x05;

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

    public final static byte UNLOCK_FLAG_UNLOCK_FIRST_LOCK_WHEN_UNLOCKED = 0x01;
    public final static byte UNLOCK_FLAG_CANCEL_WAIT_LOCK_WHEN_UNLOCKED  = 0x02;
    public final static byte UNLOCK_FLAG_FROM_AOF                        = 0x04;

    public final static int TIMEOUT_FLAG_PUSH_SUBSCRIBE                        = 0x0020;
    public final static int TIMEOUT_FLAG_MINUTE_TIME                           = 0x0040;
    public final static int TIMEOUT_FLAG_REVERSE_KEY_LOCK_WHEN_TIMEOUT         = 0x0080;
    public final static int TIMEOUT_FLAG_UNRENEW_EXPRIED_TIME_WHEN_TIMEOUT     = 0x0100;
    public final static int TIMEOUT_FLAG_LOCK_WAIT_WHEN_UNLOCK                 = 0x0200;
    public final static int TIMEOUT_FLAG_MILLISECOND_TIME                      = 0x0400;
    public final static int TIMEOUT_FLAG_LOG_ERROR_WHEN_TIMEOUT                = 0x0800;
    public final static int TIMEOUT_FLAG_REQUIRE_ACKED                         = 0x1000;
    public final static int TIMEOUT_FLAG_UPDATE_NO_RESET_TIMEOUT_CHECKED_COUNT = 0x2000;
    public final static int TIMEOUT_FLAG_KEEPLIVED                             = 0x8000;

    public final static int EXPRIED_FLAG_PUSH_SUBSCRIBE                        = 0x0020;
    public final static int EXPRIED_FLAG_MINUTE_TIME                           = 0x0040;
    public final static int EXPRIED_FLAG_REVERSE_KEY_LOCK_WHEN_EXPRIED         = 0x0080;
    public final static int EXPRIED_FLAG_ZEOR_AOF_TIME                         = 0x0100;
    public final static int EXPRIED_FLAG_UNLIMITED_AOF_TIME                    = 0x0200;
    public final static int EXPRIED_FLAG_MILLISECOND_TIME                      = 0x0400;
    public final static int EXPRIED_FLAG_LOG_ERROR_WHEN_EXPRIED                = 0x0800;
    public final static int EXPRIED_FLAG_AOF_TIME_OF_EXPRIED_PARCENT           = 0x1000;
    public final static int EXPRIED_FLAG_UPDATE_NO_RESET_EXPRIED_CHECKED_COUNT = 0x2000;
    public final static int EXPRIED_FLAG_UNLIMITED_EXPRIED_TIME                = 0x4000;
    public final static int EXPRIED_FLAG_KEEPLIVED                             = 0x8000;

    int getCommandType();
    byte[] getRequestId();
    ICommand parseCommand(byte[] buf);
    byte[] dumpCommand();
}
