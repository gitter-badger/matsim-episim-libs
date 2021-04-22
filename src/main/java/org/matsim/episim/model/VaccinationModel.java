package org.matsim.episim.model;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimPerson;

import java.util.Map;

/**
 * Vaccination model executed every iterations.
 */
public interface VaccinationModel {

	/**
	 * Perform vaccinations on the given persons
	 *
	 * @param persons               all existing persons.
	 * @param reVaccination         whether this is for vaccination
	 * @param availableVaccinations number of vaccinations available for this day.
	 * @param iteration             current iteration
	 * @param now                   current time (start of day)
	 * @return number of people vaccinated
	 */
	int handleVaccination(Map<Id<Person>, EpisimPerson> persons, boolean reVaccination, int availableVaccinations, int iteration, double now);

	/**
	 * Set vaccination status of a person.
	 */
	default void vaccinate(EpisimPerson p, int iteration, boolean reVaccination) {
		if (reVaccination)
			p.setReVaccinationStatus(EpisimPerson.VaccinationStatus.yes, iteration);
		else
			p.setVaccinationStatus(EpisimPerson.VaccinationStatus.yes, iteration);
	}

}
