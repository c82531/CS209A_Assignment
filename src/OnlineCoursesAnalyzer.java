import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * This is just a demo for you, please run it on JDK17 (some statements may be not allowed in lower version).
 * This is just a demo, and you can extend and implement functions
 * based on this demo, or implement it in a different way.
 */
public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1], new Date(info[2]), info[3], info[4], info[5],
                        Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                        Integer.parseInt(info[9]), Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                        Double.parseDouble(info[12]), Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                        Double.parseDouble(info[15]), Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                        Double.parseDouble(info[18]), Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                        Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        Map<String, Integer> ptcpByInst = courses.stream()
                .collect(Collectors.groupingBy(Course::getInstitution, TreeMap::new,
                       Collectors.summingInt(Course::getParticipants)));
        return ptcpByInst;
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> ptcpCountByInstAndSubject = courses.stream()
                .collect(Collectors.groupingBy(course -> course.getInstitution() + "-" + course.getSubject(),
                        Collectors.summingInt(Course::getParticipants)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        return ptcpCountByInstAndSubject;
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<List<String>>> courseListOfInstructor = courses.stream()
                .flatMap(course -> Arrays.stream(course.getInstructors().split(","))
                        .map(instructor -> new AbstractMap.SimpleEntry<>(instructor.trim(), course)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.collectingAndThen(
                                Collectors.partitioningBy(
                                        entry -> entry.getValue().getInstructors().split(",").length == 1,
                                        Collectors.mapping(entry -> entry.getValue().getTitle(), Collectors.toSet())
                                ),
                                map -> Arrays.asList(
                                        map.get(true).stream().sorted().collect(Collectors.toList()),
                                        map.get(false).stream().sorted().collect(Collectors.toList())
                                )
                        )
                ));
        return courseListOfInstructor;
    }

    //4
    public List<String> getCourses(int topK, String by) {
        List<String> topKCourseTitles = null;
        if (by.equals("hours")) {
            topKCourseTitles = courses.stream()
                    .sorted(Comparator.comparingDouble(Course::getTotalHours)
                            .reversed()
                            .thenComparing(Course::getTitle))
                    .map(Course::getTitle)
                    .distinct()
                    .limit(topK)
                    .collect(Collectors.toList());
        } else if (by.equals("participants")) {
            topKCourseTitles = courses.stream()
                    .sorted(Comparator.comparingInt(Course::getParticipants)
                            .reversed()
                            .thenComparing(Course::getTitle))
                    .map(Course::getTitle)
                    .distinct()
                    .limit(topK)
                    .collect(Collectors.toList());
        }
        return topKCourseTitles;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        List<String> coursesMatched = courses.stream().filter(course -> course
                        .getSubject().toLowerCase().contains(courseSubject.toLowerCase())
                        && course.getPercentAudited() >= percentAudited
        && course.getTotalHours() <= totalCourseHours)
                .map(Course::getTitle)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return coursesMatched;
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        List<String> recommendList = courses.stream()
                .collect(Collectors.groupingBy(Course::getNumber))
                .values().stream()
                .map(courseList -> {
                    String latestCourse = courseList.stream()
                            .max(Comparator.comparing(Course::getLaunchDate))
                            .map(Course::getTitle)
                            .orElse("");
                    double avgMedianAge = courseList.stream()
                            .mapToDouble(Course::getMedianAge)
                            .average()
                            .orElse(0);
                    double avgMale = courseList.stream()
                            .mapToDouble(Course::getPercentMale)
                            .average()
                            .orElse(0);
                    double avgBachelorsOrHigher = courseList.stream()
                            .mapToDouble(Course::getPercentDegree)
                            .average()
                            .orElse(0);
                    double similarityValue = Math.pow(age - avgMedianAge, 2) +
                            Math.pow(gender * 100 - avgMale, 2) +
                            Math.pow(isBachelorOrHigher * 100 - avgBachelorsOrHigher, 2);
                    return new AbstractMap.SimpleEntry<>(latestCourse, similarityValue);
                })
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
        return recommendList;
    }

}

class Course {
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
                  String title, String instructors, String subject,
                  int year, int honorCode, int participants,
                  int audited, int certified, double percentAudited,
                  double percentCertified, double percentCertified50,
                  double percentVideo, double percentForum, double gradeHigherZero,
                  double totalHours, double medianHoursCertification,
                  double medianAge, double percentMale, double percentFemale,
                  double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) title = title.substring(1);
        if (title.endsWith("\"")) title = title.substring(0, title.length() - 1);
        this.title = title;
        if (instructors.startsWith("\"")) instructors = instructors.substring(1);
        if (instructors.endsWith("\"")) instructors = instructors.substring(0, instructors.length() - 1);
        this.instructors = instructors;
        if (subject.startsWith("\"")) subject = subject.substring(1);
        if (subject.endsWith("\"")) subject = subject.substring(0, subject.length() - 1);
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }

    public String getInstitution() {
        return institution;
    }

    public int getParticipants() {
        return participants;
    }

    public String getNumber() {
        return number;
    }

    public Date getLaunchDate() {
        return launchDate;
    }

    public String getTitle() {
        return title;
    }

    public String getInstructors() {
        return instructors;
    }

    public String getSubject() {
        return subject;
    }

    public int getYear() {
        return year;
    }

    public int getHonorCode() {
        return honorCode;
    }

    public int getAudited() {
        return audited;
    }

    public int getCertified() {
        return certified;
    }

    public double getPercentAudited() {
        return percentAudited;
    }

    public double getPercentCertified() {
        return percentCertified;
    }

    public double getPercentCertified50() {
        return percentCertified50;
    }

    public double getPercentVideo() {
        return percentVideo;
    }

    public double getPercentForum() {
        return percentForum;
    }

    public double getGradeHigherZero() {
        return gradeHigherZero;
    }

    public double getTotalHours() {
        return totalHours;
    }

    public double getMedianHoursCertification() {
        return medianHoursCertification;
    }

    public double getMedianAge() {
        return medianAge;
    }

    public double getPercentMale() {
        return percentMale;
    }

    public double getPercentFemale() {
        return percentFemale;
    }

    public double getPercentDegree() {
        return percentDegree;
    }
}