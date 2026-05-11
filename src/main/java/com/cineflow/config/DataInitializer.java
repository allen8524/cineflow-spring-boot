package com.cineflow.config;

import com.cineflow.domain.Booking;
import com.cineflow.domain.BookingSeat;
import com.cineflow.domain.BookingStatus;
import com.cineflow.domain.Movie;
import com.cineflow.domain.MovieStatus;
import com.cineflow.domain.Payment;
import com.cineflow.domain.PaymentMethod;
import com.cineflow.domain.PaymentStatus;
import com.cineflow.domain.Schedule;
import com.cineflow.domain.ScheduleSeat;
import com.cineflow.domain.Screen;
import com.cineflow.domain.SeatTemplate;
import com.cineflow.domain.SeatType;
import com.cineflow.domain.Theater;
import com.cineflow.repository.BookingRepository;
import com.cineflow.repository.BookingSeatRepository;
import com.cineflow.repository.MovieRepository;
import com.cineflow.repository.PaymentRepository;
import com.cineflow.repository.ScheduleRepository;
import com.cineflow.repository.ScheduleSeatRepository;
import com.cineflow.repository.ScreenRepository;
import com.cineflow.repository.SeatTemplateRepository;
import com.cineflow.repository.TheaterRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Configuration
public class DataInitializer {

    private static final String JURASSIC_PARK_TITLE = "쥬라기 공원";
    private static final String GODFATHER_TITLE = "대부";
    private static final String IT_TITLE = "그것";
    private static final String SKYFALL_TITLE = "007 스카이폴";
    private static final String INTERSTELLAR_TITLE = "인터스텔라";
    private static final String INCEPTION_TITLE = "인셉션";
    private static final String DARK_KNIGHT_TITLE = "다크 나이트";
    private static final String JURASSIC_PARK_POSTER_URL = "https://image.tmdb.org/t/p/w500/b1xCNnyrPebIc7EWNZIa6jhb1Ww.jpg";
    private static final String GODFATHER_POSTER_URL = "https://image.tmdb.org/t/p/w500/3bhkrj58Vtu7enYsRolD1fZdja1.jpg";
    private static final String IT_POSTER_URL = "https://image.tmdb.org/t/p/w500/9E2y5Q7WlCVNEhP5GiVTjhEhx1o.jpg";
    private static final String SKYFALL_POSTER_URL = "https://image.tmdb.org/t/p/w500/d0IVecFQvsGdSbnMAHqiYsNYaJT.jpg";
    private static final String INTERSTELLAR_POSTER_URL = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg";
    private static final String INCEPTION_POSTER_URL = "https://image.tmdb.org/t/p/w500/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg";
    private static final String DARK_KNIGHT_POSTER_URL = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg";

    @Bean
    @ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
    CommandLineRunner initData(
            MovieRepository movieRepository,
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            TheaterRepository theaterRepository,
            ScreenRepository screenRepository,
            ScheduleRepository scheduleRepository,
            SeatTemplateRepository seatTemplateRepository,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        return args -> {
            initializeMovies(movieRepository);
            initializeCinemaStructure(movieRepository, theaterRepository, screenRepository, scheduleRepository);
            initializeSeatTemplates(screenRepository, seatTemplateRepository);
            initializeScheduleSeats(scheduleRepository, seatTemplateRepository, scheduleSeatRepository);
            initializeSeedBookings(bookingRepository, bookingSeatRepository, paymentRepository, scheduleRepository, scheduleSeatRepository);
            syncScheduleAvailability(scheduleRepository, scheduleSeatRepository);
        };
    }

    private void initializeMovies(MovieRepository movieRepository) {
        if (movieRepository.count() > 0) {
            return;
        }

        movieRepository.saveAll(List.of(
                Movie.builder()
                        .tmdbId(329L)
                        .title(JURASSIC_PARK_TITLE)
                        .shortDescription("공룡이 되살아난 테마파크에서 펼쳐지는 스티븐 스필버그의 모험 블록버스터.")
                        .description("최첨단 유전공학으로 공룡을 복원한 외딴 섬의 테마파크. 정식 개장을 앞둔 검증 투어 도중 보안 시스템이 무너지며 방문객들은 살아 움직이는 공룡들 사이에서 탈출해야 한다.")
                        .overview("A wealthy entrepreneur secretly creates a theme park featuring living dinosaurs drawn from prehistoric DNA. Before opening day, he invites experts and family members to preview the park, but a security breakdown turns wonder into survival.")
                        .genre("모험 · SF")
                        .ageRating("12")
                        .runningTime(127)
                        .runtimeMinutes(127)
                        .posterPath("/b1xCNnyrPebIc7EWNZIa6jhb1Ww.jpg")
                        .backdropPath("/fQ8n091t3P6pYqkWZt4IARz5t2c.jpg")
                        .posterUrl(JURASSIC_PARK_POSTER_URL)
                        .bookingRate(31.2)
                        .score(9.1)
                        .releaseDate(LocalDate.of(1993, 6, 11))
                        .status(MovieStatus.NOW_SHOWING)
                        .bookingOpen(true)
                        .build(),
                Movie.builder()
                        .tmdbId(238L)
                        .title(GODFATHER_TITLE)
                        .shortDescription("코를레오네 패밀리의 권력과 가족, 배신을 그린 범죄 영화의 고전.")
                        .description("뉴욕 마피아 가문 코를레오네 패밀리의 수장 비토와 전쟁 영웅으로 돌아온 막내아들 마이클. 가족을 둘러싼 폭력과 거래가 깊어질수록 마이클은 피하려 했던 세계의 중심으로 들어선다.")
                        .overview("Spanning the years 1945 to 1955, the story follows the Corleone crime family and Michael Corleone's transformation from reluctant outsider to ruthless family leader.")
                        .genre("범죄 · 드라마")
                        .ageRating("19")
                        .runningTime(175)
                        .runtimeMinutes(175)
                        .posterPath("/3bhkrj58Vtu7enYsRolD1fZdja1.jpg")
                        .backdropPath("/tmU7GeKVybMWFButWEGl2M4GeiP.jpg")
                        .posterUrl(GODFATHER_POSTER_URL)
                        .bookingRate(24.8)
                        .score(9.2)
                        .releaseDate(LocalDate.of(1972, 3, 14))
                        .status(MovieStatus.NOW_SHOWING)
                        .bookingOpen(true)
                        .build(),
                Movie.builder()
                        .tmdbId(346364L)
                        .title(IT_TITLE)
                        .shortDescription("데리 마을의 아이들이 공포의 존재 페니와이즈에 맞서는 호러 드라마.")
                        .description("아이들이 하나둘 사라지는 마을 데리. 루저 클럽이라 불리는 아이들은 각자의 두려움을 먹고 자라는 광대 페니와이즈의 실체를 마주하고, 함께 맞서기로 결심한다.")
                        .overview("In a small town in Maine, seven children known as The Losers Club face life problems, bullies and a monster that takes the shape of a clown called Pennywise.")
                        .genre("공포 · 드라마")
                        .ageRating("15")
                        .runningTime(135)
                        .runtimeMinutes(135)
                        .posterPath("/9E2y5Q7WlCVNEhP5GiVTjhEhx1o.jpg")
                        .backdropPath("/tcheoA2nPATCm2vvXw2hVQoaEFD.jpg")
                        .posterUrl(IT_POSTER_URL)
                        .bookingRate(19.7)
                        .score(8.5)
                        .releaseDate(LocalDate.of(2017, 9, 6))
                        .status(MovieStatus.NOW_SHOWING)
                        .bookingOpen(true)
                        .build(),
                Movie.builder()
                        .tmdbId(37724L)
                        .title(SKYFALL_TITLE)
                        .shortDescription("본드의 과거와 MI6의 현재가 충돌하는 샘 멘데스 연출의 첩보 액션.")
                        .description("작전 실패 이후 MI6가 공격받고 M의 과거가 조직 전체를 위협한다. 제임스 본드는 몸과 신뢰를 회복하며 정체를 드러낸 적과 마지막 대결을 준비한다.")
                        .overview("When Bond's latest assignment goes wrong and agents around the world are exposed, MI6 comes under attack. Bond must track down and destroy the threat, no matter how personal the cost.")
                        .genre("액션 · 스릴러")
                        .ageRating("15")
                        .runningTime(143)
                        .runtimeMinutes(143)
                        .posterPath("/d0IVecFQvsGdSbnMAHqiYsNYaJT.jpg")
                        .backdropPath("/mMZRKb3NVo5ZeSPEIaNW9buLWQ0.jpg")
                        .posterUrl(SKYFALL_POSTER_URL)
                        .bookingRate(14.2)
                        .score(8.2)
                        .releaseDate(LocalDate.of(2012, 10, 24))
                        .status(MovieStatus.NOW_SHOWING)
                        .bookingOpen(true)
                        .build(),
                Movie.builder()
                        .tmdbId(157336L)
                        .title(INTERSTELLAR_TITLE)
                        .shortDescription("인류의 미래를 위해 웜홀 너머로 향하는 우주 탐사와 가족의 이야기.")
                        .description("황폐해진 지구에서 인류의 생존 가능성을 찾기 위해 전직 조종사 쿠퍼는 미지의 은하로 떠난다. 시간과 중력, 가족에 대한 약속이 거대한 선택의 무게가 된다.")
                        .overview("The adventures of a group of explorers who use a newly discovered wormhole to surpass the limitations on human space travel and seek a future for humankind.")
                        .genre("SF · 드라마")
                        .ageRating("12")
                        .runningTime(169)
                        .runtimeMinutes(169)
                        .posterPath("/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg")
                        .backdropPath("/xJHokMbljvjADYdit5fK5VQsXEG.jpg")
                        .posterUrl(INTERSTELLAR_POSTER_URL)
                        .bookingRate(12.4)
                        .score(9.0)
                        .releaseDate(LocalDate.of(2014, 11, 5))
                        .status(MovieStatus.COMING_SOON)
                        .bookingOpen(false)
                        .build(),
                Movie.builder()
                        .tmdbId(27205L)
                        .title(INCEPTION_TITLE)
                        .shortDescription("꿈속에 침투해 생각을 훔치는 전문가가 불가능한 임무에 도전한다.")
                        .description("타인의 꿈에 들어가 비밀을 빼내는 코브는 모든 것을 되돌릴 수 있는 마지막 기회를 얻는다. 이번 임무는 정보를 훔치는 것이 아니라 한 사람의 마음에 생각을 심는 것이다.")
                        .overview("A skilled thief who steals corporate secrets through dream-sharing technology is given a chance to have his past erased if he can plant an idea into a target's subconscious.")
                        .genre("액션 · SF")
                        .ageRating("12")
                        .runningTime(148)
                        .runtimeMinutes(148)
                        .posterPath("/oYuLEt3zVCKq57qu2F8dT7NIa6f.jpg")
                        .backdropPath("/s3TBrRGB1iav7gFOCNx3H31MoES.jpg")
                        .posterUrl(INCEPTION_POSTER_URL)
                        .bookingRate(8.1)
                        .score(8.9)
                        .releaseDate(LocalDate.of(2010, 7, 15))
                        .status(MovieStatus.COMING_SOON)
                        .bookingOpen(false)
                        .build(),
                Movie.builder()
                        .tmdbId(155L)
                        .title(DARK_KNIGHT_TITLE)
                        .shortDescription("고담을 뒤흔드는 조커와 배트맨의 충돌을 그린 슈퍼히어로 범죄 드라마.")
                        .description("범죄와 부패를 몰아내려는 배트맨, 고든, 하비 덴트 앞에 예측 불가능한 조커가 나타난다. 고담의 질서와 신념은 혼돈 속에서 가장 어려운 시험을 맞는다.")
                        .overview("Batman raises the stakes in his war on crime with the help of Lieutenant Jim Gordon and District Attorney Harvey Dent, until the Joker unleashes chaos across Gotham City.")
                        .genre("액션 · 범죄")
                        .ageRating("15")
                        .runningTime(152)
                        .runtimeMinutes(152)
                        .posterPath("/qJ2tW6WMUDux911r6m7haRef0WH.jpg")
                        .backdropPath("/hkBaDkMWbLaf8B1lsWsKX7Ew3Xq.jpg")
                        .posterUrl(DARK_KNIGHT_POSTER_URL)
                        .bookingRate(7.5)
                        .score(9.0)
                        .releaseDate(LocalDate.of(2008, 7, 16))
                        .status(MovieStatus.COMING_SOON)
                        .bookingOpen(false)
                        .build()
        ));
    }

    private void initializeCinemaStructure(
            MovieRepository movieRepository,
            TheaterRepository theaterRepository,
            ScreenRepository screenRepository,
            ScheduleRepository scheduleRepository
    ) {
        if (theaterRepository.count() > 0 || screenRepository.count() > 0 || scheduleRepository.count() > 0) {
            return;
        }

        List<Movie> movies = movieRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        if (movies.size() < 5) {
            throw new IllegalStateException("상영시간표를 만들기 위한 영화 데이터가 충분하지 않습니다.");
        }

        Movie jurassicPark = movies.get(0);
        Movie godfather = movies.get(1);
        Movie it = movies.get(2);
        Movie skyfall = movies.get(3);
        Movie interstellar = movies.get(4);

        Theater gangnam = theaterRepository.save(Theater.builder()
                .name("CineFlow 강남")
                .location("서울특별시 강남구 테헤란로 410")
                .region("서울")
                .description("프리미엄 IMAX와 심야 상영이 강점인 대표 지점")
                .build());

        Theater hongdae = theaterRepository.save(Theater.builder()
                .name("CineFlow 홍대")
                .location("서울특별시 마포구 양화로 176")
                .region("서울")
                .description("젊은 관객층이 많은 도심형 멀티플렉스")
                .build());

        Theater jamsil = theaterRepository.save(Theater.builder()
                .name("CineFlow 잠실")
                .location("서울특별시 송파구 올림픽로 300")
                .region("서울")
                .description("돌비 사운드와 가족 관람 수요가 많은 복합관")
                .build());

        Screen gangnamImax = screenRepository.save(Screen.builder()
                .theater(gangnam)
                .name("1관")
                .screenType("IMAX")
                .totalSeats(120)
                .build());

        Screen gangnamStandard = screenRepository.save(Screen.builder()
                .theater(gangnam)
                .name("2관")
                .screenType("2D")
                .totalSeats(120)
                .build());

        Screen hongdaeLaser = screenRepository.save(Screen.builder()
                .theater(hongdae)
                .name("3관")
                .screenType("LASER")
                .totalSeats(120)
                .build());

        Screen hongdaeStandard = screenRepository.save(Screen.builder()
                .theater(hongdae)
                .name("5관")
                .screenType("2D")
                .totalSeats(120)
                .build());

        Screen jamsil4dx = screenRepository.save(Screen.builder()
                .theater(jamsil)
                .name("6관")
                .screenType("4DX")
                .totalSeats(120)
                .build());

        Screen jamsilDolby = screenRepository.save(Screen.builder()
                .theater(jamsil)
                .name("8관")
                .screenType("DOLBY ATMOS")
                .totalSeats(120)
                .build());

        scheduleRepository.saveAll(List.of(
                createSchedule(jurassicPark, gangnamImax, LocalDateTime.of(2026, 4, 3, 10, 20), 22000, 58),
                createSchedule(jurassicPark, hongdaeLaser, LocalDateTime.of(2026, 4, 4, 14, 5), 18000, 73),
                createSchedule(jurassicPark, jamsilDolby, LocalDateTime.of(2026, 4, 5, 19, 40), 19000, 44),

                createSchedule(godfather, hongdaeStandard, LocalDateTime.of(2026, 4, 3, 12, 45), 15000, 81),
                createSchedule(godfather, gangnamStandard, LocalDateTime.of(2026, 4, 4, 19, 40), 15000, 67),
                createSchedule(godfather, jamsilDolby, LocalDateTime.of(2026, 4, 6, 17, 20), 19000, 35),

                createSchedule(it, jamsil4dx, LocalDateTime.of(2026, 4, 3, 15, 30), 23000, 29),
                createSchedule(it, hongdaeLaser, LocalDateTime.of(2026, 4, 5, 20, 10), 18000, 51),

                createSchedule(skyfall, gangnamStandard, LocalDateTime.of(2026, 4, 4, 9, 30), 15000, 92),
                createSchedule(skyfall, hongdaeStandard, LocalDateTime.of(2026, 4, 6, 13, 5), 15000, 63),

                createSchedule(interstellar, jamsilDolby, LocalDateTime.of(2026, 4, 18, 18, 30), 19000, 120),
                createSchedule(interstellar, gangnamImax, LocalDateTime.of(2026, 4, 19, 11, 0), 22000, 101)
        ));
    }

    private void initializeSeatTemplates(ScreenRepository screenRepository, SeatTemplateRepository seatTemplateRepository) {
        if (seatTemplateRepository.count() > 0) {
            return;
        }

        List<SeatTemplate> seatTemplates = new ArrayList<>();
        for (Screen screen : screenRepository.findAll()) {
            for (char row = 'A'; row <= 'J'; row++) {
                for (int number = 1; number <= 12; number++) {
                    seatTemplates.add(SeatTemplate.builder()
                            .screen(screen)
                            .seatRow(String.valueOf(row))
                            .seatNumber(number)
                            .seatCode(row + String.valueOf(number))
                            .seatType(resolveSeatType(row, number))
                            .active(true)
                            .build());
                }
            }
        }

        seatTemplateRepository.saveAll(seatTemplates);
    }

    private void initializeScheduleSeats(
            ScheduleRepository scheduleRepository,
            SeatTemplateRepository seatTemplateRepository,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        if (scheduleSeatRepository.count() > 0) {
            return;
        }

        List<ScheduleSeat> scheduleSeats = new ArrayList<>();
        for (Schedule schedule : scheduleRepository.findByActiveTrueOrderByStartTimeAsc()) {
            List<SeatTemplate> seatTemplates = seatTemplateRepository.findByScreenIdAndActiveTrueOrderBySeatRowAscSeatNumberAsc(
                    schedule.getScreen().getId()
            );

            for (SeatTemplate seatTemplate : seatTemplates) {
                scheduleSeats.add(ScheduleSeat.builder()
                        .schedule(schedule)
                        .seatTemplate(seatTemplate)
                        .reserved(false)
                        .held(false)
                        .priceOverride(resolveSeatPrice(schedule.getPrice(), seatTemplate.getSeatType()))
                        .build());
            }
        }

        scheduleSeatRepository.saveAll(scheduleSeats);
    }

    private void initializeSeedBookings(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            ScheduleRepository scheduleRepository,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        if (bookingRepository.count() > 0 || paymentRepository.count() > 0) {
            return;
        }

        List<Schedule> schedules = scheduleRepository.findByActiveTrueOrderByStartTimeAsc();

        Schedule jurassicParkGangnam = findSchedule(schedules, JURASSIC_PARK_TITLE, LocalDateTime.of(2026, 4, 3, 10, 20));
        Schedule jurassicParkJamsil = findSchedule(schedules, JURASSIC_PARK_TITLE, LocalDateTime.of(2026, 4, 5, 19, 40));
        Schedule godfatherGangnam = findSchedule(schedules, GODFATHER_TITLE, LocalDateTime.of(2026, 4, 4, 19, 40));

        createBookingWithSeats(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                scheduleSeatRepository,
                jurassicParkGangnam,
                "CF20260403-1020-4H8K",
                "김민서",
                "010-1234-5678",
                List.of("E7", "E8"),
                BookingStatus.BOOKED,
                PaymentMethod.CARD,
                44000
        );

        createBookingWithSeats(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                scheduleSeatRepository,
                godfatherGangnam,
                "CF20260404-1940-T8M2",
                "김민서",
                "010-1234-5678",
                List.of("H10", "H11", "H12"),
                BookingStatus.BOOKED,
                PaymentMethod.KAKAO_PAY,
                45000
        );

        createBookingWithSeats(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                scheduleSeatRepository,
                jurassicParkJamsil,
                "CF20260405-1940-A1Q9",
                "김민서",
                "010-1234-5678",
                List.of("C4"),
                BookingStatus.BOOKED,
                PaymentMethod.NAVER_PAY,
                19000
        );

        createStandaloneBooking(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                "CF20260320-2100-F5F6",
                IT_TITLE,
                IT_POSTER_URL,
                "15",
                "CineFlow 잠실",
                "6관",
                "4DX",
                List.of("F5", "F6"),
                2,
                46000,
                LocalDateTime.of(2026, 3, 20, 21, 0),
                LocalDateTime.of(2026, 3, 20, 23, 4),
                BookingStatus.USED,
                PaymentMethod.CARD
        );

        createStandaloneBooking(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                "CF20260309-1830-G8",
                SKYFALL_TITLE,
                SKYFALL_POSTER_URL,
                "15",
                "CineFlow 홍대",
                "5관",
                "2D",
                List.of("G8"),
                1,
                15000,
                LocalDateTime.of(2026, 3, 9, 18, 30),
                LocalDateTime.of(2026, 3, 9, 20, 31),
                BookingStatus.USED,
                PaymentMethod.TOSS
        );

        createCanceledBooking(
                bookingRepository,
                bookingSeatRepository,
                paymentRepository,
                "CF20260402-1810-CN01",
                JURASSIC_PARK_TITLE,
                JURASSIC_PARK_POSTER_URL,
                "12",
                "CineFlow 강남",
                "2관",
                "2D",
                List.of("D5", "D6"),
                2,
                30000,
                LocalDateTime.of(2026, 4, 2, 18, 10),
                LocalDateTime.of(2026, 4, 2, 20, 8),
                "일정 변경"
        );

        for (Schedule schedule : schedules) {
            reserveAdditionalSeatsToTarget(schedule, schedule.getAvailableSeats(), scheduleSeatRepository);
        }
    }

    private void syncScheduleAvailability(ScheduleRepository scheduleRepository, ScheduleSeatRepository scheduleSeatRepository) {
        List<Schedule> schedules = scheduleRepository.findByActiveTrueOrderByStartTimeAsc();
        for (Schedule schedule : schedules) {
            schedule.setAvailableSeats((int) scheduleSeatRepository.countByScheduleIdAndReservedFalse(schedule.getId()));
        }
        scheduleRepository.saveAll(schedules);
    }

    private Schedule createSchedule(Movie movie, Screen screen, LocalDateTime startTime, int price, int availableSeats) {
        return Schedule.builder()
                .movie(movie)
                .screen(screen)
                .startTime(startTime)
                .endTime(startTime.plusMinutes(movie.getRunningTime()))
                .price(price)
                .availableSeats(availableSeats)
                .active(true)
                .build();
    }

    private SeatType resolveSeatType(char row, int number) {
        if (row == 'A' && number >= 5 && number <= 8) {
            return SeatType.PREMIUM;
        }
        if (row == 'J' && number >= 4 && number <= 9) {
            return SeatType.COUPLE;
        }
        return SeatType.STANDARD;
    }

    private int resolveSeatPrice(int schedulePrice, SeatType seatType) {
        return switch (seatType) {
            case PREMIUM -> schedulePrice + 3000;
            case COUPLE -> schedulePrice + 5000;
            case STANDARD -> schedulePrice;
        };
    }

    private Schedule findSchedule(List<Schedule> schedules, String title, LocalDateTime startTime) {
        return schedules.stream()
                .filter(schedule -> schedule.getMovie().getTitle().equals(title))
                .filter(schedule -> schedule.getStartTime().equals(startTime))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("기준 상영시간표를 찾을 수 없습니다. title=" + title + ", startTime=" + startTime));
    }

    private void createBookingWithSeats(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            ScheduleSeatRepository scheduleSeatRepository,
            Schedule schedule,
            String bookingCode,
            String customerName,
            String customerPhone,
            List<String> seatCodes,
            BookingStatus status,
            PaymentMethod paymentMethod,
            int totalPrice
    ) {
        List<ScheduleSeat> selectedSeats = scheduleSeatRepository.findByScheduleIdAndSeatTemplateSeatCodeIn(schedule.getId(), seatCodes);
        selectedSeats.forEach(scheduleSeat -> scheduleSeat.setReserved(true));
        scheduleSeatRepository.saveAll(selectedSeats);

        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(bookingCode)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .movieTitle(schedule.getMovie().getTitle())
                .posterUrl(schedule.getMovie().getPosterUrl())
                .ageRating(schedule.getMovie().getAgeRating())
                .theaterName(schedule.getScreen().getTheater().getName())
                .screenName(schedule.getScreen().getName())
                .screenType(schedule.getScreen().getScreenType())
                .seatNames(String.join(", ", seatCodes))
                .peopleCount(seatCodes.size())
                .totalPrice(totalPrice)
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .schedule(schedule)
                .status(status)
                .build());

        bookingSeatRepository.saveAll(selectedSeats.stream()
                .map(scheduleSeat -> BookingSeat.builder()
                        .booking(booking)
                        .seatCode(scheduleSeat.getSeatTemplate().getSeatCode())
                        .seatRow(scheduleSeat.getSeatTemplate().getSeatRow())
                        .seatNumber(scheduleSeat.getSeatTemplate().getSeatNumber())
                        .seatType(scheduleSeat.getSeatTemplate().getSeatType())
                        .price(scheduleSeat.getPriceOverride())
                        .build())
                .toList());

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .method(paymentMethod)
                .amount(totalPrice)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(schedule.getStartTime().minusHours(2))
                .transactionId("SEED-" + bookingCode.substring(Math.max(0, bookingCode.length() - 8)).toUpperCase(Locale.ROOT))
                .build());
    }

    private void createStandaloneBooking(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            String bookingCode,
            String movieTitle,
            String posterUrl,
            String ageRating,
            String theaterName,
            String screenName,
            String screenType,
            List<String> seatCodes,
            int peopleCount,
            int totalPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BookingStatus status,
            PaymentMethod paymentMethod
    ) {
        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(bookingCode)
                .customerName("김민서")
                .customerPhone("010-1234-5678")
                .movieTitle(movieTitle)
                .posterUrl(posterUrl)
                .ageRating(ageRating)
                .theaterName(theaterName)
                .screenName(screenName)
                .screenType(screenType)
                .seatNames(String.join(", ", seatCodes))
                .peopleCount(peopleCount)
                .totalPrice(totalPrice)
                .startTime(startTime)
                .endTime(endTime)
                .status(status)
                .build());

        int seatPrice = totalPrice / Math.max(1, seatCodes.size());
        bookingSeatRepository.saveAll(seatCodes.stream()
                .map(this::parseSeatCode)
                .map(parsedSeat -> BookingSeat.builder()
                        .booking(booking)
                        .seatCode(parsedSeat.seatCode())
                        .seatRow(parsedSeat.seatRow())
                        .seatNumber(parsedSeat.seatNumber())
                        .seatType(SeatType.STANDARD)
                        .price(seatPrice)
                        .build())
                .toList());

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .method(paymentMethod)
                .amount(totalPrice)
                .paymentStatus(PaymentStatus.PAID)
                .paidAt(startTime.minusHours(3))
                .transactionId("SEED-" + bookingCode.substring(Math.max(0, bookingCode.length() - 8)).toUpperCase(Locale.ROOT))
                .build());
    }

    private void createCanceledBooking(
            BookingRepository bookingRepository,
            BookingSeatRepository bookingSeatRepository,
            PaymentRepository paymentRepository,
            String bookingCode,
            String movieTitle,
            String posterUrl,
            String ageRating,
            String theaterName,
            String screenName,
            String screenType,
            List<String> seatCodes,
            int peopleCount,
            int totalPrice,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String cancelReason
    ) {
        Booking booking = bookingRepository.save(Booking.builder()
                .bookingCode(bookingCode)
                .customerName("관리 샘플")
                .customerPhone("010-9999-0000")
                .movieTitle(movieTitle)
                .posterUrl(posterUrl)
                .ageRating(ageRating)
                .theaterName(theaterName)
                .screenName(screenName)
                .screenType(screenType)
                .seatNames(String.join(", ", seatCodes))
                .peopleCount(peopleCount)
                .totalPrice(totalPrice)
                .startTime(startTime)
                .endTime(endTime)
                .status(BookingStatus.CANCELED)
                .cancelReason(cancelReason)
                .canceledAt(startTime.minusDays(1))
                .build());

        int seatPrice = totalPrice / Math.max(1, seatCodes.size());
        bookingSeatRepository.saveAll(seatCodes.stream()
                .map(this::parseSeatCode)
                .map(parsedSeat -> BookingSeat.builder()
                        .booking(booking)
                        .seatCode(parsedSeat.seatCode())
                        .seatRow(parsedSeat.seatRow())
                        .seatNumber(parsedSeat.seatNumber())
                        .seatType(SeatType.STANDARD)
                        .price(seatPrice)
                        .build())
                .toList());

        paymentRepository.save(Payment.builder()
                .booking(booking)
                .method(PaymentMethod.CARD)
                .amount(totalPrice)
                .paymentStatus(PaymentStatus.CANCELED)
                .paidAt(startTime.minusDays(2))
                .canceledAt(startTime.minusDays(1))
                .transactionId("SEED-" + bookingCode.substring(Math.max(0, bookingCode.length() - 8)).toUpperCase(Locale.ROOT))
                .cancelTransactionId("CANCEL-" + bookingCode.substring(Math.max(0, bookingCode.length() - 6)).toUpperCase(Locale.ROOT))
                .build());
    }

    private void reserveAdditionalSeatsToTarget(
            Schedule schedule,
            int targetAvailableSeats,
            ScheduleSeatRepository scheduleSeatRepository
    ) {
        List<ScheduleSeat> seats = scheduleSeatRepository.findByScheduleIdOrderBySeatTemplateSeatRowAscSeatTemplateSeatNumberAsc(schedule.getId());
        int safeTargetAvailable = Math.max(0, Math.min(targetAvailableSeats, seats.size()));
        int targetReservedCount = seats.size() - safeTargetAvailable;
        int currentReservedCount = (int) seats.stream().filter(ScheduleSeat::isReserved).count();

        if (currentReservedCount >= targetReservedCount) {
            return;
        }

        seats.stream()
                .filter(scheduleSeat -> !scheduleSeat.isReserved())
                .sorted(Comparator
                        .comparing((ScheduleSeat scheduleSeat) -> scheduleSeat.getSeatTemplate().getSeatRow())
                        .thenComparing(scheduleSeat -> scheduleSeat.getSeatTemplate().getSeatNumber()))
                .limit(targetReservedCount - currentReservedCount)
                .forEach(scheduleSeat -> scheduleSeat.setReserved(true));

        scheduleSeatRepository.saveAll(seats);
    }

    private ParsedSeat parseSeatCode(String seatCode) {
        String seatRow = seatCode.replaceAll("\\d", "");
        int seatNumber = Integer.parseInt(seatCode.replaceAll("\\D", ""));
        return new ParsedSeat(seatCode, seatRow, seatNumber);
    }

    private record ParsedSeat(String seatCode, String seatRow, int seatNumber) {
    }
}
