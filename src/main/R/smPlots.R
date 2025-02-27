library(tidyverse)
library(lubridate)
library (RCurl)

# Weather Data
weatherData <- read_delim("https://bulk.meteostat.net/daily/10382.csv.gz", delim = ",", col_names = FALSE, col_types = cols(
  X1 = col_date(format = ""),
  X2 = col_double(),
  X3 = col_double(),
  X4 = col_double(),
  X5 = col_double(),
  X6 = col_double(),
  X7 = col_double(),
  X8 = col_double(),
  X9 = col_double(),
  X10 = col_double(),
  X11 = col_double()
)) 
colnames(weatherData) <- c("date", "tavg", "tmin", "tmax", "prcp", "snow", "wdir", "wspd", "wpgt", "pres", "tsun")

weatherDataByWeek <- weatherData %>%
  mutate( week = paste0(isoweek(date), "-", isoyear(date))) %>%
  group_by( week ) %>%
  summarize( date=mean(date), tavg=mean(tavg), tmin=mean(tmin), tmax=mean(tmax), prcp=mean(prcp), snow=mean(snow), wdir=mean(wdir), wspd=mean(wspd), wpgt=mean(wpgt), pres=mean(pres), tsun=mean(tsun))

weatherDataAvg20002020 <- weatherData %>%
  mutate( year = isoyear(date)) %>%
  filter( year > 1999 & year < 2021) %>%
  mutate( monthDay = paste0 (month(date), "-", day(date))) %>%
  group_by( monthDay ) %>%
  summarise_each(funs(mean(., na.rm = TRUE)))
write_csv(weatherDataAvg20002020, "weatherDataAvg2000-2020.csv")


# Senozon Restrictions
snzRestrictionsFile <- "BerlinSnzData_daily_until20210111.csv"
svnLocation <- "/Users/sebastianmuller/git/shared-svn/"

snzRestrictions <- read_delim(paste0(svnLocation, "projects/episim/matsim-files/snz/BerlinV2/episim-input/", snzRestrictionsFile), delim = "\t") %>%
  pivot_longer(!date, names_to = "type", values_to = "restriction") %>%
  mutate(newDate = as.Date(strptime(date, "%Y%m%d"))) %>%
  mutate(weekday = wday(newDate, week_start = 1)) %>%
  filter(weekday < "6") %>%
  filter(type == "notAtHomeExceptLeisureAndEdu" | type == "notAtHomeExceptEdu" | type == "notAtHome") %>%
  mutate( week = paste0(isoweek(newDate), "-", isoyear(newDate))) %>%
  group_by( week, type ) %>%
  summarize( restriction=mean(restriction), newDate=mean(newDate))


# Google Mobility Report
googleMobilityReport <- read_csv(getURL("https://www.gstatic.com/covid19/mobility/Global_Mobility_Report.csv")) %>%
  filter(sub_region_1 == "Berlin") %>%
  select(-c(country_region_code, country_region, sub_region_1, sub_region_2, metro_area, iso_3166_2_code, census_fips_code)) %>%
  pivot_longer(!date, names_to = "type", values_to = "restriction") %>%
  # filter(type == "retail_and_recreation_percent_change_from_baseline" | type == "parks_percent_change_from_baseline" | type == "workplaces_percent_change_from_baseline") %>%
  mutate(weekday = wday(date, week_start = 1)) %>%
  filter(weekday < "6") %>%
  mutate( week = paste0(isoweek(date), "-", isoyear(date))) %>%
  group_by( week, type ) %>%
  summarize( restriction=mean(restriction), date=mean(date))

# "retail_and_recreation_percent_change_from_baseline","grocery_and_pharmacy_percent_change_from_baseline","parks_percent_change_from_baseline","transit_stations_percent_change_from_baseline","workplaces_percent_change_from_baseline","residential_percent_change_from_baseline"


# Apple Mobility Trends
appleMobilityReport <- read_csv(getURL("https://covid19-static.cdn-apple.com/covid19-mobility-data/2023HotfixDev28/v3/en-us/applemobilitytrends-2021-01-03.csv")) %>%
  filter(region == "Berlin") %>%
  select(-c(geo_type, region, alternative_name, "sub-region", country)) %>%
  pivot_longer(!transportation_type, names_to = "date", values_to = "restriction") %>%
  mutate(newDate = as.Date(strptime(date, "%Y-%m-%d"))) %>%
  mutate(weekday = wday(date, week_start = 1)) %>%
  filter(weekday < "6") %>%
  mutate( week = paste0(isoweek(newDate), "-", isoyear(newDate))) %>%
  group_by( week, transportation_type ) %>%
  summarize( restriction=mean(restriction), newDate=mean(newDate))


# rValues
run <- "calibration198"
temp <- tempfile()
download.file(paste0("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/episim/battery/2021-01-04/weather/summaries/", run, ".zip"),temp)
rValues <- read_delim(unzip(temp, paste0(run, ".rValues.txt.csv")), delim = "\t")
unlink(temp)

rValues <- rValues %>%
  select(-c(day, rValue, newContagious, scenario)) %>%
  pivot_longer(!date, names_to = "type", values_to = "rValue") %>%
  mutate( week = paste0(isoweek(date), "-", isoyear(date))) %>%
  group_by( week, type ) %>%
  summarize( rValue=mean(rValue), date=mean(date))



# --- PLOTS ---



# Senozon Restrictions
ggplot(data = snzRestrictions, mapping=aes(x = newDate)) +
  labs(
    title="Reduction of out of home activities in Berlin based on mobile phone data",
    caption="Source: Senozon, VSP TU Berlin",
    x="date", y="Reduction") +
  geom_point(mapping=aes(y = restriction, colour = type)) +
  ylim(-50, 25) +
  theme(legend.position = "bottom")
  geom_vline(xintercept = as.Date("2021-01-06"))


# Google Mobility Report 
ggplot(data = googleMobilityReport, mapping=aes(x = date)) +
  geom_point(mapping=aes(y = restriction, colour = type)) +
  theme(legend.position = "bottom") +
  geom_vline(xintercept = as.Date("2020-10-22"))


# Google Mobility and weather
ggplot() +
  geom_point(data = googleMobilityReport, mapping=aes(x = date, y = restriction, colour = type)) +
  geom_point(data = weatherDataByWeek, mapping=aes(x = date, y = tmax)) +
  theme(legend.position = "bottom") +
  xlim(c(as.Date("2020-02-24"), as.Date("2020-12-31")))


# Apple Mobility Trends
ggplot(data = appleMobilityReport, mapping=aes(x = newDate)) +
  geom_point(mapping=aes(y = restriction, colour = transportation_type)) +
  theme(legend.position = "bottom") +
geom_vline(xintercept = as.Date("2020-12-24"))


# rValues per Activity
ggplot(data = rValues, mapping=aes(x = date,y = rValue, fill = factor(type, levels=c("pt", "other", "leisure", "day care", "schools", "university", "work&business", "home")))) + 
  geom_bar(position="stack", stat="identity") +
  geom_line(mapping=aes(colour=type)) +
  xlim(c(as.Date("2020-02-24"), as.Date("2020-12-31"))) +
  labs(fill = "Activity")



