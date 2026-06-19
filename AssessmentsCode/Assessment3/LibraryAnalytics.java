package service;

import entity.Book;

import java.util.*;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LibraryAnalytics {

    private Map<String, Book> books = new HashMap<>();


    public void loadBooks(List<String> records) {

        books.clear();

        if(records == null) {
            return;
        }

        for(String record : records) {

            String[] arr = record.split("\\|");

            if(arr.length != 6) {
                continue;
            }

            String bookId = arr[0].trim();
            String title = arr[1].trim();
            String author = arr[2].trim();
            String category = arr[3].trim();
            String borrowStr = arr[4].trim();
            String ratingStr = arr[5].trim();

            if(bookId.isEmpty()
                    || title.isEmpty()
                    || author.isEmpty()
                    || category.isEmpty()
                    || borrowStr.isEmpty()
                    || ratingStr.isEmpty()) {

                continue;
            }

            try {

                int borrowCount = Integer.parseInt(borrowStr);      // typecasting
                double rating = Double.parseDouble(ratingStr);

                if(rating < 0 || rating > 5 || borrowCount < 0) {
                    continue;
                }

                Book current =
                        new Book(bookId,
                                title,
                                author,
                                category,
                                borrowCount,
                                rating);



                Book existing = books.get(bookId);

                if (existing == null) {
                    books.put(bookId, current);
                } else {
                    books.put(bookId, chooseBest(existing, current));
                }


            } catch(Exception e) {

            }
        }
    }



    private Book chooseBest(Book b1, Book b2) {

        if(b1.getRating() != b2.getRating()) {
            return b1.getRating() > b2.getRating() ? b1 : b2;
        }

        if(b1.getBorrowCount() != b2.getBorrowCount()) {
            return b1.getBorrowCount() > b2.getBorrowCount()
                    ? b1
                    : b2;
        }

        return b1.getTitle().compareTo(b2.getTitle()) <= 0
                ? b1
                : b2;
    }

    /*
     ------------------------------------------------
     RULE 3
     ------------------------------------------------
    */

    public List<Book> topRatedBooks(int n) {

        if (n <= 0) {
            return Collections.emptyList();
        }


        return books.values()
                .stream()
                .sorted(
                        Comparator
                                .comparingDouble(Book::getRating)
                                .reversed()
                                .thenComparing(
                                        Comparator.comparingInt(Book::getBorrowCount)
                                                .reversed())
                                .thenComparing(Book::getBookId)
                )
                .limit(n)
                .toList();
    }

    /*
     ------------------------------------------------
     RULE 4
     ------------------------------------------------
    */

    public Map<String, Double> averageRatingByCategory() {

        return books.values()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Book::getCategory,
                                TreeMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.averagingDouble(Book::getRating),
                                        avg ->
                                                Math.round(avg * 100.0) / 100.0
                                )
                        )
                );
    }

    /*
     ------------------------------------------------
     RULE 5
     ------------------------------------------------
    */

    public Optional<Book> mostBorrowedBook() {

        return books.values()
                .stream()
                .sorted(
                        Comparator
                                .comparingInt(Book::getBorrowCount)
                                .reversed()
                                .thenComparing(
                                        Comparator.comparingDouble(Book::getRating)
                                                .reversed()
                                )
                                .thenComparing(Book::getBookId)
                )
                .findFirst();
    }

    /*
     ------------------------------------------------
     RULE 6
     ------------------------------------------------
    */

    public Set<String> authorsWithMultipleCategories() {

        return books.values()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Book::getAuthor,
                                Collectors.mapping(
                                        Book::getCategory,
                                        Collectors.toSet()
                                )
                        )
                )
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /*
     ------------------------------------------------
     RULE 7
     ------------------------------------------------
    */

    public Map<String,List<Book>> groupBooksByAuthor() {

        return books.values()
                .stream()
                .sorted(
                        Comparator.comparing(Book::getAuthor)
                )
                .collect(
                        Collectors.groupingBy(
                                Book::getAuthor,
                                LinkedHashMap::new,
                                Collectors.collectingAndThen(
                                        Collectors.toList(),
                                        list ->
                                                list.stream()
                                                        .sorted(
                                                                Comparator
                                                                        .comparingDouble(Book::getRating)
                                                                        .reversed()
                                                                        .thenComparing(
                                                                                Comparator.comparingInt(Book::getBorrowCount)
                                                                                        .reversed()
                                                                        )
                                                        )
                                                        .collect(Collectors.toList())
                                )
                        )
                );
    }

    /*
     ------------------------------------------------
     RULE 8
     ------------------------------------------------
    */

    public List<String> suspiciousBooks() {

        Map<String, Double> avgBorrow =
                books.values()
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        Book::getCategory,
                                        Collectors.averagingInt(Book::getBorrowCount)
                                )
                        );

        Pattern repeatedWord =
                Pattern.compile("\\b(\\w+)\\s+\\1\\b",
                        Pattern.CASE_INSENSITIVE);

        return books.values()
                .stream()
                .filter(book -> {

                    boolean condition1 =
                            repeatedWord
                                    .matcher(book.getTitle())
                                    .find();

                    boolean condition2 =
                            book.getTitle()
                                    .toLowerCase()
                                    .contains(
                                            book.getAuthor()
                                                    .toLowerCase()
                                    );

                    double categoryAvgBorrow =
                            avgBorrow.get(book.getCategory());

                    boolean condition3 =
                            book.getBorrowCount()
                                    > categoryAvgBorrow * 4;

                    List<Book> sameCategory =
                            books.values()
                                    .stream()
                                    .filter(b -> b.getCategory().equals(book.getCategory()))
                                    .toList();

                    boolean condition4 = false;
                    if (sameCategory.size() > 1) {
                        int sumBorrowOthers =
                                sameCategory.stream().mapToInt(Book::getBorrowCount).sum()
                                        - book.getBorrowCount();

                        double sumRatingOthers =
                                sameCategory.stream().mapToDouble(Book::getRating).sum()
                                        - book.getRating();

                        double otherAvgBorrow = sumBorrowOthers / (double) (sameCategory.size() - 1);
                        double otherAvgRating = sumRatingOthers / (sameCategory.size() - 1);

                        condition4 =
                                book.getRating() < otherAvgRating
                                        && book.getBorrowCount() > otherAvgBorrow;
                    }

                    return condition1
                            || condition2
                            || condition3
                            || condition4;
                })
                .map(Book::getTitle)
                .distinct()
                .sorted()
                .toList();
    }

    /*
     ------------------------------------------------
     FINAL CHALLENGE
     ------------------------------------------------
    */

    public Map<String, Map<String, Book>>
    categoryWiseTopRatedBookByEachAuthor() {

        Comparator<Book> comparator =
                Comparator
                        .comparingDouble(Book::getRating)
                        .reversed()
                        .thenComparing(
                                Comparator.comparingInt(Book::getBorrowCount)
                                        .reversed()
                        )
                        .thenComparing(Book::getTitle);

        return books.values()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                Book::getCategory,
                                TreeMap::new,

                                Collectors.toMap(
                                        Book::getAuthor,

                                        Function.identity(),

                                        (b1,b2) ->
                                                comparator.compare(b1,b2) <= 0
                                                        ? b1
                                                        : b2,

                                        TreeMap::new
                                )
                        )
                );
    }
}

