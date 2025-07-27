USE timetracking;

DROP PROCEDURE IF EXISTS generate_fake_logs;

CREATE PROCEDURE generate_fake_logs()
BEGIN
    DECLARE emp INT;
    DECLARE dt DATE;
    SET dt = '2024-07-24';

    WHILE dt <= '2025-07-24' DO
        IF DAYOFWEEK(dt) BETWEEN 2 AND 6 THEN
            SET emp = 1;
            WHILE emp <= 12 DO
                INSERT INTO time_logs (empid, punch_in, punch_out)
                VALUES (
                    emp,
                    TIMESTAMP(dt, SEC_TO_TIME(32400 + FLOOR(RAND() * 900))),
                    TIMESTAMP(dt, SEC_TO_TIME(61200 + FLOOR(RAND() * 900)))
                );
                SET emp = emp + 1;
            END WHILE;
        END IF;
        SET dt = DATE_ADD(dt, INTERVAL 1 DAY);
    END WHILE;
END;
