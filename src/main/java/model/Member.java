package model;

import java.util.ArrayList;
import java.util.List;

/** 회원 */
public class Member {
    public String id;
    public String name;
    public String role;
    public Card creditCard;
    public Points points = new Points();
    public List<String> loginHistory = new ArrayList<>();
    public List<Payment> paymentHistory = new ArrayList<>();
    public Pending pendingCalculation;
}
