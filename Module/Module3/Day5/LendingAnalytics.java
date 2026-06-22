package org.nbfc.assignment3.service;

import org.nbfc.assignment3.model.LoanApplication;
import org.nbfc.assignment3.repository.LoanApplicationRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class LendingAnalytics {

    private final LoanApplicationRepository repository;

    public LendingAnalytics(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    public void loadApplications(List<String> records) {
        if (records == null) {
            return;
        }

        records.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .map(line -> line.split("\\|", -1))
                .filter(parts -> parts.length == 6)
                .map(parts -> {
                    try {
                        return new LoanApplication(
                                parts[0].trim(),
                                parts[1].trim(),
                                parts[2].trim(),
                                parts[3].trim(),
                                Double.parseDouble(parts[4].trim()),
                                Integer.parseInt(parts[5].trim())
                        );
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(this::isValid)
                .forEach(this::saveResolvingDuplicates);
    }

    private void saveResolvingDuplicates(LoanApplication incoming) {
        LoanApplication existing = repository.findById(incoming.getApplicationId()).orElse(null);
        if (existing == null) {
            repository.save(incoming);
            return;
        }
        repository.save(pickPreferred(existing, incoming));
    }

    private LoanApplication pickPreferred(LoanApplication existing, LoanApplication incoming) {
        if (incoming.getCreditScore() != existing.getCreditScore()) {
            return incoming.getCreditScore() > existing.getCreditScore() ? incoming : existing;
        }
        if (Double.compare(incoming.getLoanAmount(), existing.getLoanAmount()) != 0) {
            return incoming.getLoanAmount() < existing.getLoanAmount() ? incoming : existing;
        }
        return incoming.getCustomerName().compareTo(existing.getCustomerName()) < 0 ? incoming : existing;
    }

    public LoanApplication createApplication(LoanApplication application) {
        if (!isValid(application)) {
            throw new IllegalArgumentException("Invalid application payload");
        }
        if (repository.existsById(application.getApplicationId())) {
            throw new IllegalArgumentException("Application ID already exists");
        }
        return repository.save(application);
    }

    public Optional<LoanApplication> getApplication(String applicationId) {
        return repository.findById(applicationId);
    }

    public List<LoanApplication> getAllApplications() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(LoanApplication::getApplicationId))
                .toList();
    }

    public LoanApplication updateApplication(String applicationId, LoanApplication application) {
        if (!repository.existsById(applicationId)) {
            throw new IllegalArgumentException("Application not found");
        }
        LoanApplication updated = new LoanApplication(
                applicationId,
                application.getCustomerName(),
                application.getLenderName(),
                application.getLoanType(),
                application.getLoanAmount(),
                application.getCreditScore()
        );
        if (!isValid(updated)) {
            throw new IllegalArgumentException("Invalid application payload");
        }
        return repository.save(updated);
    }

    public boolean deleteApplication(String applicationId) {
        if (!repository.existsById(applicationId)) {
            return false;
        }
        repository.deleteById(applicationId);
        return true;
    }

    public void clearApplications() {
        repository.deleteAll();
    }

    public List<LoanApplication> topCreditProfiles(int n) {
        if (n <= 0) {
            return List.of();
        }

        return repository.findAll().stream()
                .sorted(Comparator.comparingInt(LoanApplication::getCreditScore)
                        .reversed()
                        .thenComparingDouble(LoanApplication::getLoanAmount)
                        .thenComparing(LoanApplication::getCustomerName))
                .limit(n)
                .toList();
    }

    public Map<String, Double> averageLoanAmountByType() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        LoanApplication::getLoanType,
                        TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.averagingDouble(LoanApplication::getLoanAmount),
                                value -> Math.round(value * 100.0) / 100.0
                        )
                ));
    }

    public Optional<LoanApplication> highestLoanApplication() {
        return repository.findAll().stream()
                .max(Comparator.comparingDouble(LoanApplication::getLoanAmount)
                        .thenComparingInt(LoanApplication::getCreditScore)
                        .thenComparing(LoanApplication::getApplicationId, Comparator.reverseOrder()));
    }

    public Set<String> lendersWithMultipleLoanTypes() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        LoanApplication::getLenderName,
                        Collectors.mapping(LoanApplication::getLoanType, Collectors.toSet())
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Map<String, List<LoanApplication>> groupApplicationsByLender() {
        Map<String, List<LoanApplication>> sortedMap = repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        LoanApplication::getLenderName,
                        TreeMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparingInt(LoanApplication::getCreditScore)
                                                .reversed()
                                                .thenComparingDouble(LoanApplication::getLoanAmount))
                                        .toList()
                        )
                ));

        return sortedMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    public List<String> suspiciousApplications() {
        List<LoanApplication> all = repository.findAll();

        Map<String, Double> averageAmountByType = all.stream()
                .collect(Collectors.groupingBy(
                        LoanApplication::getLoanType,
                        Collectors.averagingDouble(LoanApplication::getLoanAmount)
                ));

        Map<String, Double> averageCreditByType = all.stream()
                .collect(Collectors.groupingBy(
                        LoanApplication::getLoanType,
                        Collectors.averagingInt(LoanApplication::getCreditScore)
                ));

        Set<String> customersWithManyLenders = all.stream()
                .collect(Collectors.groupingBy(
                        app -> app.getCustomerName().trim().toLowerCase(),
                        Collectors.mapping(app -> app.getLenderName().trim().toLowerCase(), Collectors.toSet())
                ))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().size() > 3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Set<String> sameTypeAmountScoreDifferentCustomer = all.stream()
                .collect(Collectors.groupingBy(
                        app -> app.getLoanType().trim().toLowerCase() + "|" + app.getLoanAmount() + "|" + app.getCreditScore()
                ))
                .values()
                .stream()
                .filter(group -> group.stream()
                        .map(app -> app.getCustomerName().trim().toLowerCase())
                        .distinct()
                        .count() > 1)
                .flatMap(List::stream)
                .map(app -> app.getCustomerName().trim().toLowerCase())
                .collect(Collectors.toSet());

        Set<String> anagramCustomersWithinLender = all.stream()
                .collect(Collectors.groupingBy(
                        app -> app.getLenderName().trim().toLowerCase(),
                        Collectors.toList()
                ))
                .values()
                .stream()
                .flatMap(group -> group.stream()
                        .collect(Collectors.groupingBy(
                                app -> app.getCustomerName().replaceAll("\\s+", "").toLowerCase().chars()
                                        .sorted()
                                        .mapToObj(ch -> String.valueOf((char) ch))
                                        .collect(Collectors.joining()),
                                Collectors.toList()
                        ))
                        .values()
                        .stream()
                        .filter(anagramGroup -> anagramGroup.stream()
                                .map(app -> app.getCustomerName().trim().toLowerCase())
                                .distinct()
                                .count() > 1)
                        .flatMap(List::stream)
                )
                .map(app -> app.getCustomerName().trim().toLowerCase())
                .collect(Collectors.toSet());

        return all.stream()
                .filter(app -> {
                    String customerName = app.getCustomerName().trim();
                    String[] words = customerName.split("\\s+");
                    String normalizedCustomer = customerName.toLowerCase();
                    String normalizedLender = app.getLenderName().trim().toLowerCase();
                    double avgAmount = averageAmountByType.getOrDefault(app.getLoanType(), 0.0);
                    double avgCredit = averageCreditByType.getOrDefault(app.getLoanType(), 0.0);

                    boolean condition1 = IntStream.range(0, Math.max(words.length - 1, 0))
                            .anyMatch(i -> words[i].equalsIgnoreCase(words[i + 1]));
                    boolean condition2 = !normalizedLender.isBlank() && normalizedCustomer.contains(normalizedLender);
                    boolean condition3 = avgAmount > 0.0 && app.getLoanAmount() > avgAmount * 2.5;
                    boolean condition4 = app.getCreditScore() < avgCredit && app.getLoanAmount() > avgAmount;
                    boolean condition5 = words.length > 3;
                    boolean condition6 = customersWithManyLenders.contains(normalizedCustomer);
                    boolean condition7 = sameTypeAmountScoreDifferentCustomer.contains(normalizedCustomer);
                    boolean condition8 = anagramCustomersWithinLender.contains(normalizedCustomer);

                    return condition1 || condition2 || condition3 || condition4
                            || condition5 || condition6 || condition7 || condition8;
                })
                .map(app -> app.getCustomerName().trim())
                .distinct()
                .sorted()
                .toList();
    }

    public Map<String, Map<String, Optional<LoanApplication>>> loanTypeWiseTopApplicantByLender() {
        Comparator<LoanApplication> topApplicantComparator = Comparator
                .comparingInt(LoanApplication::getCreditScore)
                .thenComparing(LoanApplication::getLoanAmount, Comparator.reverseOrder())
                .thenComparing(LoanApplication::getCustomerName, Comparator.reverseOrder());

        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        LoanApplication::getLoanType,
                        TreeMap::new,
                        Collectors.groupingBy(
                                LoanApplication::getLenderName,
                                TreeMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.mapping(Function.identity(), Collectors.maxBy(topApplicantComparator)),
                                        result -> result
                                )
                        )
                ));
    }

    private boolean isValid(LoanApplication application) {
        return application != null
                && application.getApplicationId() != null
                && !application.getApplicationId().trim().isEmpty()
                && application.getCustomerName() != null
                && !application.getCustomerName().trim().isEmpty()
                && application.getLenderName() != null
                && !application.getLenderName().trim().isEmpty()
                && application.getLoanType() != null
                && !application.getLoanType().trim().isEmpty()
                && application.getLoanAmount() > 0
                && application.getCreditScore() >= 300
                && application.getCreditScore() <= 900;
    }
}
