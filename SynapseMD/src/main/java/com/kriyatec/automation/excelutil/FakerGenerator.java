package com.kriyatec.automation.excelutil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import com.github.javafaker.Faker;

public class FakerGenerator {

    private final ThreadLocal<Faker> threadLocalFaker = ThreadLocal.withInitial(() -> new Faker());

    public Faker getFaker() {
        return threadLocalFaker.get();
    }

    private Random random = new Random();

    // Method to get data based on Faker function
    public String getFakerData(String value) {

        // Trim and convert to lowercase for case-insensitive matching
        String trimmedValue = value != null ? value.trim().toLowerCase() : "";

        // Handle random.date with offset and format (e.g., random.date:5:dd MMMM yyyy)
        if (trimmedValue.startsWith("random.date:")) {
            String params = value.substring(value.indexOf(':') + 1).trim();
            String[] parts = params.split(":");

            if (parts.length == 2) {
                // Format: random.date:offset:format
                try {
                    int offset = Integer.parseInt(parts[0].trim());
                    String format = parts[1].trim();
                    return generateDateWithOffset(offset, format);
                } catch (NumberFormatException e) {
                    return generateCurrentDate();
                }
            } else if (parts.length == 1) {
                // Format: random.date:offset (use default format)
                try {
                    int offset = Integer.parseInt(parts[0].trim());
                    return generateDateWithOffset(offset);
                } catch (NumberFormatException e) {
                    return generateCurrentDate();
                }
            }
        }

        switch (trimmedValue) {
            case "random.longname":
            case "longname":
                return generateLongName();
            case "random.name":
                return getFaker().name().firstName(); // Full name
            case "random.address":
                return generateRandomAddress(); // Random address
            case "address":
                return generateRandomAddress();
            case "random.phone":
                return generateMobileNumber(); // Random phone number
            case "random.email":
                return generateEmail(); // Random email
            case "random.number":
                return String.valueOf((int) (Math.random() * 10000));
            case "random.establishedyear":
                return generateEstablishedYear();
            case "random.id":
                return generateId();
            case "random.registerno":
                return generateRegistrationNumber();
            case "random.organizationname":
                return generateOrganizationName();
            case "random.website":
                return generateWebsiteLink(); // Random website link
            case "random.facebook":
                return generateSocialLink("facebook"); // Random Facebook profile link
            case "random.instagram":
                return generateSocialLink("instagram"); // Random Instagram profile link
            case "random.linkedin":
                return generateSocialLink("linkedin"); // Random LinkedIn profile link
            case "random.youtube":
                return generateSocialLink("youtube"); // Random YouTube channel link
            case "random.code":
                return getFaker().bothify("???###").toUpperCase(); // Generates a random subject code like "ABC123"
            case "random.year":
                return String.valueOf(getFaker().number().numberBetween(1900, 2024));
            case "random.description":
                return getFaker().lorem().paragraph();
            case "random.count":
                return String.valueOf(getFaker().number().numberBetween(1, 50));
            case "random.planname":
                String[] planPrefixes = { "Basic", "Standard", "Premium", "Elite", "Essential", "Advanced",
                        "Comprehensive", "Family", "Senior", "Care" };
                String[] planSuffixes = { "Health Plan", "Wellness Package", "Care Program", "Medical Plan",
                        "Coverage Plan", "Health Shield", "Wellness Plan", "Care Package" };
                return planPrefixes[getFaker().number().numberBetween(0, planPrefixes.length)]
                        + " " + planSuffixes[getFaker().number().numberBetween(0, planSuffixes.length)];
            case "random.plandescription":
                String[] planDesc = {
                        "A comprehensive health plan designed to cover routine and specialist consultations.",
                        "Provides essential medical coverage for doctors, diagnostics, and preventive care.",
                        "An all-inclusive wellness package tailored for families and senior citizens.",
                        "Covers outpatient consultations, lab tests, and specialist referrals for better health management.",
                        "A premium plan offering unlimited access to specialists, teleconsultations, and wellness programs."
                };
                return planDesc[getFaker().number().numberBetween(0, planDesc.length)];
            case "random.tagname":
                String[] tags = { "Chronic Care", "Preventive", "Wellness", "Acute Care", "Post-Surgery", "Oncology",
                        "Cardiology", "Neurology", "Orthopedics", "Pediatrics", "Geriatrics", "Diabetes",
                        "Hypertension" };
                return tags[getFaker().number().numberBetween(0, tags.length)];
            case "random.categoryname":
                String[] categories = { "Mental Health", "General Medicine", "Dermatology", "Dentistry",
                        "Ophthalmology",
                        "Physiotherapy", "Nutrition & Diet", "Gynecology", "ENT", "Urology", "Pulmonology",
                        "Endocrinology" };
                return categories[getFaker().number().numberBetween(0, categories.length)];
            case "random.categorydescription":
                String[] catDesc = {
                        "Focuses on the prevention, diagnosis, and treatment of disorders related to this specialty.",
                        "Provides expert medical consultation and management for patients requiring specialized care.",
                        "Covers a broad range of conditions handled by qualified specialists in this domain.",
                        "Dedicated to improving patient outcomes through evidence-based practices in this category.",
                        "Offers comprehensive assessment and treatment plans tailored to each patient's needs."
                };
                return catDesc[getFaker().number().numberBetween(0, catDesc.length)];
            case "random.amount":
                return String.valueOf(getFaker().number().numberBetween(10000, 500000));
            case "random.policyname":
                String[] policyPrefixes = { "Silver", "Gold", "Platinum", "Diamond", "Star", "Secure",
                        "Active", "Family", "Super", "Optima" };
                String[] policySuffixes = { "Shield", "Care", "Secure", "Guard", "Assure", "Health", "Premium" };
                return policyPrefixes[getFaker().number().numberBetween(0, policyPrefixes.length)]
                        + " " + policySuffixes[getFaker().number().numberBetween(0, policySuffixes.length)]
                        + " Plan";
            case "random.policynumber":
                return getFaker().number().digits(10);
            case "random.surgeryname":
                String[] surgeries = { "Appendectomy", "Cataract Surgery", "Coronary Artery Bypass", 
                        "Knee Replacement", "Tonsillectomy", "Cholecystectomy (Gallbladder)", 
                        "Inguinal Hernia Repair", "Cesarean Section", "Hip Replacement", "Spinal Fusion" };
                return surgeries[getFaker().number().numberBetween(0, surgeries.length)];
            case "random.allergy":
                String[] allergies = { "Penicillin Allergy", "Peanut Allergy", "Shellfish Allergy", 
                        "Lactose Intolerance", "Sulfa Drugs Allergy", "Dust Mites Allergy", 
                        "Pollen Allergy", "Latex Allergy", "Mold Allergy", "Aspirin Allergy" };
                return allergies[getFaker().number().numberBetween(0, allergies.length)];
            case "random.medicinename":
                String[] medicines = { "Amoxicillin", "Atorvastatin", "Metformin", "Lisinopril", 
                        "Levothyroxine", "Albuterol", "Amlodipine", "Gabapentin", "Omeprazole", "Losartan" };
                return medicines[getFaker().number().numberBetween(0, medicines.length)];
            case "random.height":
                return String.valueOf(getFaker().number().numberBetween(140, 200));
            case "random.weight":
                return String.valueOf(getFaker().number().numberBetween(45, 120));
            case "random.date":
                return generateCurrentDate();

            // Add other cases as needed
            default:
                return value; // Return original value instead of "Invalid Faker Method"
        }
    }

    // Generate current date (dd MMMM yyyy format by default)
    public String generateCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy");
        return sdf.format(new Date());
    }

    // Generate date with offset from current date (dd MMMM yyyy format by default)
    public String generateDateWithOffset(int days) {
        return generateDateWithOffset(days, "dd MMMM yyyy");
    }

    // Generate date with offset and custom format
    public String generateDateWithOffset(int days, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        long currentMillis = System.currentTimeMillis();
        long offsetMillis = currentMillis + ((long) days * 24 * 60 * 60 * 1000);
        return sdf.format(new Date(offsetMillis));
    }

    // Unified method to generate social media links based on platform
    public String generateSocialLink(String platform) {
        String username = getFaker().name().username().replaceAll("[^a-zA-Z0-9_]", ""); // Clean username, allow
                                                                                        // underscores
        switch (platform.toLowerCase()) {
            case "facebook":
                return "https://www.facebook.com/" + username; // e.g., "https://www.facebook.com/johndoe"
            case "instagram":
                return "https://www.instagram.com/" + username; // e.g., "https://www.instagram.com/johndoe"
            case "linkedin":
                String firstName = getFaker().name().firstName().toLowerCase().replaceAll("[^a-z0-9]", "");
                String lastName = getFaker().name().lastName().toLowerCase().replaceAll("[^a-z0-9]", "");
                String uniqueId = String.format("%04d", random.nextInt(10000)); // 4-digit number for uniqueness
                String profileSlug = firstName + "-" + lastName + "-" + uniqueId;
                return "https://www.linkedin.com/in/" + profileSlug; // e.g.,
                                                                     // "https://www.linkedin.com/in/john-doe-1234"
            case "youtube":
                return "https://www.youtube.com/@" + username; // e.g., "https://www.youtube.com/@johndoe"
            // Add more platforms here as needed, e.g., twitter, tiktok, etc.
            default:
                return "https://www.example.com/" + username; // Fallback to a generic link
        }
    }

    // New methods for generating random social media and website links
    public String generateWebsiteLink() {
        String domainName = getFaker().internet().domainName(); // e.g., "example.com"
        String protocol = random.nextBoolean() ? "https://" : "http://"; // Randomly choose protocol
        String prefix = random.nextBoolean() ? "www." : ""; // Optionally add "www"
        return protocol + prefix + domainName; // e.g., "https://www.example.com" or "http://example.com"
    }

    public String generateFacebookProfileLink() {
        String username = getFaker().name().username().replaceAll("[^a-zA-Z0-9]", ""); // Clean username, e.g.,
                                                                                       // "john.doe" → "johndoe"
        return "https://www.facebook.com/" + username; // e.g., "https://www.facebook.com/johndoe"
    }

    public String generateInstagramProfileLink() {
        String username = getFaker().name().username().replaceAll("[^a-zA-Z0-9_]", ""); // Allow underscores, e.g.,
                                                                                        // "john.doe" → "johndoe"
        return "https://www.instagram.com/" + username; // e.g., "https://www.instagram.com/johndoe"
    }

    public String generateLinkedInProfileLink() {
        String firstName = getFaker().name().firstName().toLowerCase().replaceAll("[^a-z0-9]", "");
        String lastName = getFaker().name().lastName().toLowerCase().replaceAll("[^a-z0-9]", "");
        String uniqueId = String.format("%04d", random.nextInt(10000)); // 4-digit number for uniqueness
        String profileSlug = firstName + "-" + lastName + "-" + uniqueId;
        return "https://www.linkedin.com/in/" + profileSlug; // e.g., "https://www.linkedin.com/in/john-doe-1234"
    }

    public String generateOrganizationName() {
        return getFaker().company().name(); // e.g., "Acme Corp", "Johnson LLC"
    }

    public String generateRegistrationNumber() {
        String number = String.format("%04d", random.nextInt(10000)); // 4-digit number (0000-9999)
        String year = String.valueOf(random.nextInt(24) + 2000); // Year between 2000 and 2023
        return number + year; // e.g., 1234-2021
    }

    // Generate a random general ID (e.g., ID-XXXXX)
    public String generateId() {
        String prefix = "ID";
        String id = String.format("%05d", random.nextInt(100000)); // 5-digit number (00000-99999)
        return prefix + "-" + id; // e.g., ID-01234
    }

    public String generateMobileNumber() {
        // Generate the first digit (6 to 9)
        int firstDigit = random.nextInt(4) + 6; // Generates either 6, 7, 8, or 9
        StringBuilder mobileNumber = new StringBuilder();

        // Append the first digit
        mobileNumber.append(firstDigit);

        // Generate the next 9 digits
        for (int i = 0; i < 9; i++) {
            int nextDigit = random.nextInt(10); // Generates a digit between 0 and 9
            mobileNumber.append(nextDigit);
        }

        return mobileNumber.toString();
    }

    // Generate a random established year (between 1900 and 2023)
    public String generateEstablishedYear() {
        int year = random.nextInt(124) + 1900; // Range: 1900 to 2023
        return String.valueOf(year); // e.g., "1985"
    }

    public String generateRandomName() {
        return getFaker().name().fullName();
    }

    public String generateLongName() {
        String[] pool1 = {
            "venkatachalpathy", "anantapadmanabhan", "thirunavukkarasu", "kalyanasundaram",
            "shankaranarayanan", "balakrishnan", "radhakrishnan", "gopalakrishnan"
        };
        String[] pool2 = {
            "somasundaram", "meenakshisundaram", "veerabhadran", "ramakrishnan",
            "ramanathan", "swaminathan", "jagadeesan", "narayanaswamy"
        };
        String[] pool3 = {
            "vijayaragavan", "venkatraman", "subramanian", "chidambaram",
            "rajagopalan", "sundararaman", "parameswaran", "viswanathan"
        };
        String[] pool4 = {
            "balaji", "murugan", "ganesh", "karthik", "senthil", "kumar", "pillai", "iyer"
        };

        String p1 = pool1[random.nextInt(pool1.length)];
        String p2 = pool2[random.nextInt(pool2.length)];
        String p3 = pool3[random.nextInt(pool3.length)];
        String p4 = pool4[random.nextInt(pool4.length)];

        return p1 + " " + p2 + " " + p3 + " " + p4;
    }

    public String generateEmail() {
        String username = getFaker().name().username().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return username + "@yopmail.com";
    }

    public String generateRandomAddress() {
        return getFaker().address().fullAddress();
    }

    // public String generateMobileNumber() {
    // return getFaker().phoneNumber().phoneNumber();
    // }
}
