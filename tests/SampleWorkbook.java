package com.suriya.expensetracker;
import java.io.*; import java.util.*;
public final class SampleWorkbook {
 public static void main(String[] args)throws Exception {
  WorkbookIO.Sheet month=new WorkbookIO.Sheet("Oct-2026");
  month.row(WorkbookIO.t("October 2026 • Dummy transactions"));
  month.row(WorkbookIO.t("Record type"),WorkbookIO.t("Date"),WorkbookIO.t("Description"),WorkbookIO.t("Bank"),WorkbookIO.t("Type"),WorkbookIO.t("Category"),WorkbookIO.t("Amount (₹)"),WorkbookIO.t("Status"));
  WorkbookIO.Sheet meta=new WorkbookIO.Sheet("App data");
  meta.row(WorkbookIO.t("Expense Tracker backup"),WorkbookIO.t("1"));
  meta.row(WorkbookIO.t("header"),WorkbookIO.t("{\"schema\":7,\"created_at\":1795536000000}"));
  month.row(WorkbookIO.t("Transaction"),WorkbookIO.t("03 Oct 2026"),WorkbookIO.t("Dummy grocery shop"),WorkbookIO.t("Demo Bank"),WorkbookIO.t("Debit"),WorkbookIO.t("Groceries"),WorkbookIO.n("1255.00"),WorkbookIO.t("Reviewed"));
  meta.row(WorkbookIO.t("transactions"),WorkbookIO.t("{\"data\":{\"_id\":1,\"amount\":125500,\"kind\":\"Expense\",\"category\":\"Groceries\",\"title\":\"Dummy grocery shop\",\"date\":1791009000000,\"review\":0,\"bank\":\"Demo Bank\"},\"local\":1,\"uid\":\"7d4767f4-3e8a-43cb-a7e3-160abb1f5ab3\",\"aliases\":[\"7d4767f4-3e8a-43cb-a7e3-160abb1f5ab3\"]}"));
  month.row(WorkbookIO.t("Transaction"),WorkbookIO.t("08 Oct 2026"),WorkbookIO.t("Dummy salary"),WorkbookIO.t("Example Bank"),WorkbookIO.t("Credit"),WorkbookIO.t("Salary"),WorkbookIO.n("25000.00"),WorkbookIO.t("Reviewed"));
  meta.row(WorkbookIO.t("transactions"),WorkbookIO.t("{\"data\":{\"_id\":2,\"amount\":2500000,\"kind\":\"Income\",\"category\":\"Salary\",\"title\":\"Dummy salary\",\"date\":1791441000000,\"review\":0,\"bank\":\"Example Bank\"},\"local\":2,\"uid\":\"64795e45-f8a5-4cec-ab19-2ec6025390fe\",\"aliases\":[\"64795e45-f8a5-4cec-ab19-2ec6025390fe\"]}"));
  month.row(WorkbookIO.t("Transaction"),WorkbookIO.t("15 Oct 2026"),WorkbookIO.t("Dummy restaurant"),WorkbookIO.t("Demo Bank"),WorkbookIO.t("Debit"),WorkbookIO.t("Food"),WorkbookIO.n("350.00"),WorkbookIO.t("Reviewed"));
  meta.row(WorkbookIO.t("transactions"),WorkbookIO.t("{\"data\":{\"_id\":3,\"amount\":35000,\"kind\":\"Expense\",\"category\":\"Food\",\"title\":\"Dummy restaurant\",\"date\":1792045800000,\"review\":0,\"bank\":\"Demo Bank\"},\"local\":3,\"uid\":\"0c2e0d61-f03f-445a-821d-dc260d21e657\",\"aliases\":[\"0c2e0d61-f03f-445a-821d-dc260d21e657\"]}"));
  month.row(WorkbookIO.t("Transaction"),WorkbookIO.t("22 Oct 2026"),WorkbookIO.t("Dummy bus ticket"),WorkbookIO.t("Example Bank"),WorkbookIO.t("Debit"),WorkbookIO.t("Transport"),WorkbookIO.n("80.00"),WorkbookIO.t("Reviewed"));
  meta.row(WorkbookIO.t("transactions"),WorkbookIO.t("{\"data\":{\"_id\":4,\"amount\":8000,\"kind\":\"Expense\",\"category\":\"Transport\",\"title\":\"Dummy bus ticket\",\"date\":1792650600000,\"review\":0,\"bank\":\"Example Bank\"},\"local\":4,\"uid\":\"4af4b1f1-3d8d-4f21-9fab-f02547d6a051\",\"aliases\":[\"4af4b1f1-3d8d-4f21-9fab-f02547d6a051\"]}"));
  try(OutputStream out=new FileOutputStream(args[0])){WorkbookIO.write(out,Arrays.asList(month,meta));}
  if(WorkbookIO.readMeta(new File(args[0])).size()!=6)throw new AssertionError("Invalid fixture");
  System.out.println("Sample backup created and metadata read successfully");
 }
}