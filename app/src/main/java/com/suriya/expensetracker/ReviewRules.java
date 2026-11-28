package com.suriya.expensetracker;
/** Only an uncategorized expense is shown as needing category review. */
final class ReviewRules {
    static boolean needsReview(String kind,String category){return "Expense".equals(kind)&& (category==null || category.trim().isEmpty() || "Uncategorized".equalsIgnoreCase(category.trim()));}
}
