package com.suriya.expensetracker;
import java.io.File;
import java.nio.charset.StandardCharsets;
public final class AndroidWorkbookSmokeTest {
    public static void main(String[] args) throws Exception {
        BackupSnapshot.Parsed backup=BackupSnapshot.validate(WorkbookIO.readMeta(new File(args[0])));
        if(backup.transactions!=4)throw new AssertionError("Expected 4 dummy transactions");
        if(backup.tables.get("emi_plans").size()!=0||backup.tables.get("fuel_cycles").size()!=0)throw new AssertionError("Unexpected EMI or fuel data");
        boolean blocked=false;
        try{WorkbookIO.xml("<?xml version=\"1.0\"?><!DOCTYPE x [<!ENTITY a SYSTEM \"file:///etc/passwd\">]><x>&a;</x>".getBytes(StandardCharsets.UTF_8));}catch(Exception expected){blocked=true;}
        if(!blocked)throw new AssertionError("Unsafe XML accepted");
        System.out.println("ANDROID_WORKBOOK_TEST_PASS: sample metadata and unsafe XML rejection");
    }
}
