package com.sqli.ecomAnalytics.generator;

import com.github.javafaker.Faker;
import com.sqli.ecomAnalytics.configuration.DataGenerationProp;
import com.sqli.ecomAnalytics.dto.CustomerRegistrationDto;
import com.sqli.ecomAnalytics.exceptions.CustomerAlreadyExistsException;
import com.sqli.ecomAnalytics.service.CustomersService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Component
public class CustomerGenerator {
    private final EntityManager entityManager;
    private final CustomersService customersService;
    private final DataGenerationProp prop;
    private final Faker faker = new Faker();


    public CustomerGenerator(EntityManager entityManager, CustomersService customersService, DataGenerationProp prop) {
        this.entityManager = entityManager;
        this.customersService = customersService;
        this.prop = prop;
    }

    private static final Random random = new Random();
    private static final int MIN_AGE = 18;
    private static final int MAX_AGE = 75;
    private static final double MEAN_AGE = 35;
    private static final double STD_DEV = 10;

    private int generateAgeNormal() {
        int age;
        do {
            age = (int) Math.round(MEAN_AGE + random.nextGaussian() * STD_DEV);
        } while (age < MIN_AGE || age > MAX_AGE);
        return age;
    }

    private String getRandomCountry(){
        var geoDist = prop.getCustomers().getGeogDist();
        int roll = random.nextInt(100);

        if (roll < geoDist.getMoroccoPerc()) {
            return "Morocco";
        } else if (roll < geoDist.getMoroccoPerc() + geoDist.getFrancePerc()) {
            return "France";
        } else if (roll < geoDist.getMoroccoPerc() + geoDist.getFrancePerc() + geoDist.getUsaPerc()) {
            return "United States";
        } else if (roll < geoDist.getMoroccoPerc() + geoDist.getFrancePerc() + geoDist.getUsaPerc() + geoDist.getCanadaPerc()) {
            return "Canada";
        } else {
            return faker.address().country();
        }
    }


    public static LocalDateTime generateRandomRegistrationDate(int maxMonths) {

        double r = random.nextDouble();
        double weighted = Math.pow(r, 2);
        int monthsBack = (int) (weighted * maxMonths);

        LocalDate baseDate = LocalDate.now().minusMonths(monthsBack);

        int day = 1 + random.nextInt(baseDate.lengthOfMonth());

        int hour = random.nextInt(24);
        int minute = random.nextInt(60);
        int second = random.nextInt(60);

        return baseDate.withDayOfMonth(day)
                .atTime(hour, minute, second);
    }

    public void generateCustomers() {
        int total = prop.getCustomers().getCount();
        for (int i = 1; i <= total; i++) {
            CustomerRegistrationDto c = new CustomerRegistrationDto();
            c.setFirstName(faker.name().firstName());
            c.setLastName(faker.name().lastName());
            c.setEmail(faker.internet().emailAddress());
            c.setAge(generateAgeNormal());
            c.setCountry(getRandomCountry());
            c.setPhone(faker.phoneNumber().phoneNumber());
            c.setRegisterDate(generateRandomRegistrationDate(24));

            try {
                customersService.registerCustomer(c);
            } catch ( CustomerAlreadyExistsException | DataIntegrityViolationException | JpaSystemException | PersistenceException e) {
                entityManager.clear();
                i--;
            }
        }
    }
}
