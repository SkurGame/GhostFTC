package org.firstinspires.ftc.teamcode.util;

/**
 * Класс для преобразования тиков энкодера в сантиметры и обратно
 */
public class EncoderConverter {

    // Конфигурационные параметры (должны быть установлены при создании объекта)
    private final double countsPerMotorRev;     // Тиков на оборот мотора
    private final double driveGearReduction;    // Коэффициент редуктора
    private final double wheelDiameterCM;       // Диаметр колеса в см
    private final double countsPerCM;           // Рассчитанные тики на см

    /**
     * Конструктор с пользовательскими параметрами
     */
    public EncoderConverter(double countsPerMotorRev, double driveGearReduction, double wheelDiameterCM) {
        this.countsPerMotorRev = countsPerMotorRev;
        this.driveGearReduction = driveGearReduction;
        this.wheelDiameterCM = wheelDiameterCM;
        this.countsPerCM = calculateCountsPerCM();
    }

    /**
     * Конструктор с параметрами по умолчанию для популярных моторов
     */
    public EncoderConverter() {
        // Параметры для NeveRest 40 Gearmotors (тележка с 4 колесами)
        this(28, 40, 10.0); // 28:1 редуктор, 40 тиков на оборот, 10 см колеса
    }

    /**
     * Рассчитывает тики на см
     */
    private double calculateCountsPerCM() {
        double countsPerWheelRev = countsPerMotorRev * driveGearReduction;
        double wheelCircumferenceCM = Math.PI * wheelDiameterCM;
        return countsPerWheelRev / wheelCircumferenceCM;
    }

    /**
     * Преобразует тики энкодера в сантиметры
     * @param ticks количество тиков
     * @return расстояние в см
     */
    public double ticksToCM(int ticks) {
        return ticks / countsPerCM;
    }

    /**
     * Преобразует тики энкодера в сантиметры
     * @param ticks количество тиков (double)
     * @return расстояние в см
     */
    public double ticksToCM(double ticks) {
        return ticks / countsPerCM;
    }

    /**
     * Преобразует сантиметры в тики энкодера
     * @param cm расстояние в см
     * @return количество тиков
     */
    public int cmToTicks(double cm) {
        return (int) Math.round(cm * countsPerCM);
    }

    /**
     * Преобразует тики энкодера в метры
     * @param ticks количество тиков
     * @return расстояние в метрах
     */
    public double ticksToMeters(int ticks) {
        return ticksToCM(ticks) / 100.0;
    }

    /**
     * Преобразует метры в тики энкодера
     * @param meters расстояние в метрах
     * @return количество тиков
     */
    public int metersToTicks(double meters) {
        return cmToTicks(meters * 100.0);
    }

    /**
     * Преобразует тики в обороты колеса
     * @param ticks количество тиков
     * @return количество оборотов колеса
     */
    public double ticksToWheelRevolutions(int ticks) {
        double countsPerWheelRev = countsPerMotorRev * driveGearReduction;
        return ticks / countsPerWheelRev;
    }

    /**
     * Преобразует обороты колеса в тики
     * @param revolutions количество оборотов
     * @return количество тиков
     */
    public int wheelRevolutionsToTicks(double revolutions) {
        double countsPerWheelRev = countsPerMotorRev * driveGearReduction;
        return (int) Math.round(revolutions * countsPerWheelRev);
    }

    // Геттеры для параметров конфигурации

    public double getCountsPerMotorRev() {
        return countsPerMotorRev;
    }

    public double getDriveGearReduction() {
        return driveGearReduction;
    }

    public double getWheelDiameterCM() {
        return wheelDiameterCM;
    }

    public double getCountsPerCM() {
        return countsPerCM;
    }

    /**
     * @return длину окружности колеса в см
     */
    public double getWheelCircumferenceCM() {
        return Math.PI * wheelDiameterCM;
    }

    /**
     * Выводит информацию о конфигурации преобразователя
     */
    @Override
    public String toString() {
        return String.format(
                "EncoderConverter[countsPerMotorRev=%.1f, gearReduction=%.1f, " +
                        "wheelDiameter=%.1fcm, countsPerCM=%.3f]",
                countsPerMotorRev, driveGearReduction, wheelDiameterCM, countsPerCM
        );
    }
}