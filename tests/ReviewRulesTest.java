package com.suriya.expensetracker;
public final class ReviewRulesTest {
 public static void main(String[] args){if(!ReviewRules.needsReview("Expense","Uncategorized"))throw new AssertionError("uncategorized expense");if(ReviewRules.needsReview("Expense","Snacks"))throw new AssertionError("categorized payee");if(ReviewRules.needsReview("Income","Uncategorized"))throw new AssertionError("credit has no category");System.out.println("Review label checks passed");}
}
