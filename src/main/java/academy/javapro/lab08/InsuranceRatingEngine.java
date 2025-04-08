/* Project: Lab8
 * Class: InsuranceRatingEngine.java
 * Author: MEHMET SOYDAN
 * Date: 04/07/2025
 * This program implements a rule-based insurance rating engine that calculates
 * insurance premiums based on driver and vehicle characteristics using a set of
 * predefined rules and a knowledge base of insurance rates.
 */
package academy.javapro.lab08;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class InsuranceRatingEngine {
    // Knowledge base (facts about insurance rates)
    private Map<String, Object> knowledgeBase = new HashMap<>();

    // Rules list
    private List<Rule> rules = new ArrayList<>();

    // ---------------------------------------------------------------
    // Constructor initializes the knowledge base and rules
    public InsuranceRatingEngine() {
        initializeKnowledgeBase();
        initializeRules();
    }

    // ---------------------------------------------------------------
    // Initializes the knowledge base with base rates, age factors, and accident surcharges
    private void initializeKnowledgeBase() {
        // Base rates by vehicle category
        knowledgeBase.put("baseRate.sedan", 1000.0);
        knowledgeBase.put("baseRate.suv", 1200.0);
        knowledgeBase.put("baseRate.luxury", 1500.0);
        knowledgeBase.put("baseRate.sports", 1800.0);

        // Age risk factors
        knowledgeBase.put("ageFactor.16-19", 2.0);
        knowledgeBase.put("ageFactor.20-24", 1.5);
        knowledgeBase.put("ageFactor.25-65", 1.0);
        knowledgeBase.put("ageFactor.66+", 1.3);

        // Accident surcharges
        knowledgeBase.put("accidentSurcharge.0", 0.0);
        knowledgeBase.put("accidentSurcharge.1", 300.0);
        knowledgeBase.put("accidentSurcharge.2+", 600.0);
    }

    // ---------------------------------------------------------------
    // Initializes the rules for premium calculation
    private void initializeRules() {
        // Base rate rule - determines the starting premium based on vehicle type
        rules.add(new Rule("base rate",
            profile -> true, // Predicate that always returns true
            (profile, premium) -> {
                String vehicleCategory = determineVehicleCategory(profile);
                double baseRate = (double) knowledgeBase.get("baseRate." + vehicleCategory);
                premium.setBaseRate(baseRate);
            }
        ));

        // Age factor rule - adjusts premium based on driver's age
        rules.add(new Rule("age factor",
            profile -> true, // Applies to all profiles
            (profile, premium) -> {
                int age = profile.getAge();
                double factor;
                String explanation;

                if (age < 20) {
                    factor = (double) knowledgeBase.get("ageFactor.16-19");
                    explanation = "Drivers under 20 have higher statistical risk";
                } else if (age < 25) {
                    factor = (double) knowledgeBase.get("ageFactor.20-24");
                    explanation = "Drivers 20-24 have moderately higher risk";
                } else if (age < 66) {
                    factor = (double) knowledgeBase.get("ageFactor.25-65");
                    explanation = "Standard rate for drivers 25-65";
                } else {
                    factor = (double) knowledgeBase.get("ageFactor.66+");
                    explanation = "Slight increase for senior drivers";
                }

                double adjustment = premium.getBaseRate() * (factor - 1.0);
                premium.addAdjustment("Age factor", adjustment, explanation);
            }
        ));

        // Accident history rule - adds surcharges for recent accidents
        rules.add(new Rule("accident history",
            profile -> profile.getAccidentsInLastFiveYears() > 0, // Only applies if accidents > 0
            (profile, premium) -> {
                int accidents = profile.getAccidentsInLastFiveYears();
                double surcharge;
                String explanation;

                if (accidents == 1) {
                    surcharge = (double) knowledgeBase.get("accidentSurcharge.1");
                    explanation = "Surcharge for 1 accident in past 5 years";
                } else {
                    surcharge = (double) knowledgeBase.get("accidentSurcharge.2+");
                    explanation = "Major surcharge for 2+ accidents in past 5 years";
                }

                premium.addAdjustment("Accident history", surcharge, explanation);
            }
        ));
    }

    // ---------------------------------------------------------------
    // Determines vehicle category based on make and model
    private String determineVehicleCategory(DriverProfile profile) {
        String make = profile.getVehicleMake().toLowerCase();
        String model = profile.getVehicleModel().toLowerCase();

        // Luxury vehicle classification
        if (make.equals("bmw") || make.equals("mercedes") || 
            make.equals("lexus") || make.equals("audi")) {
            return "luxury";
        }

        // Sports car classification
        if (make.equals("ferrari") || make.equals("porsche") || 
            make.equals("mustang") || make.equals("corvette")) {
            return "sports";
        }

        // SUV classification
        if (model.equals("suv") || model.equals("explorer") || 
            model.equals("tahoe") || model.equals("highlander")) {
            return "suv";
        }

        // Default to sedan
        return "sedan";
    }

    // ---------------------------------------------------------------
    // Calculates premium by applying all applicable rules
    public Premium calculatePremium(DriverProfile profile) {
        Premium premium = new Premium();

        // Apply all rules that match the profile
        for (Rule rule : rules) {
            if (rule.matches(profile)) {
                rule.apply(profile, premium);
            }
        }

        return premium;
    }

    // ---------------------------------------------------------------
    // Inner class representing an insurance rating rule
    static class Rule {
        private String name;
        private Predicate<DriverProfile> condition;
        private BiConsumer<DriverProfile, Premium> action;

        public Rule(String name, Predicate<DriverProfile> condition, 
                   BiConsumer<DriverProfile, Premium> action) {
            this.name = name;
            this.condition = condition;
            this.action = action;
        }

        public boolean matches(DriverProfile profile) {
            return condition.test(profile);
        }

        public void apply(DriverProfile profile, Premium premium) {
            action.accept(profile, premium);
        }

        public String getName() {
            return name;
        }
    }
}
