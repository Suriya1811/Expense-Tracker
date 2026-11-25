package com.suriya.expensetracker;
import java.io.*;import java.util.*;
public final class WorkbookIOTest {
 public static void main(String[] args)throws Exception{
  WorkbookIO.Sheet month=new WorkbookIO.Sheet("Nov 2026");month.row(WorkbookIO.t("Transactions"));month.row(WorkbookIO.t("Food & tea"),WorkbookIO.n("22.50"));
  WorkbookIO.Sheet meta=new WorkbookIO.Sheet("App data");meta.row(WorkbookIO.t("Expense Tracker backup"),WorkbookIO.t("1"));meta.row(WorkbookIO.t("sample"),WorkbookIO.t("{\"name\":\"tea & rice\"}"));
  File f=new File(args[0]);try(OutputStream o=new FileOutputStream(f)){WorkbookIO.write(o,Arrays.asList(month,meta));}ArrayList<String[]> rows=WorkbookIO.readMeta(f);
  if(rows.size()!=2||!rows.get(1)[1].contains("tea & rice"))throw new AssertionError();System.out.println("Excel workbook round trip passed");
 }
}
