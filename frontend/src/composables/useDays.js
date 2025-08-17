export const useDays = () => {
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
    
    const getDayName = (dayInt) => days[dayInt]
    const getDayInt = (dayName) => days.indexOf(dayName)
    
    return { days, getDayName, getDayInt }
}