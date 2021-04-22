package org.matsim.episim.model;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.episim.EpisimPerson;
import org.matsim.episim.EpisimUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Vaccinate people starting with oldest first
 */
public class VaccinationByAge implements VaccinationModel {

	private final SplittableRandom rnd;

	private final int MAX_AGE = 130;

	private final int MINIMUM_AGE_FOR_VACCINATIONS = 6;

	@Inject
	public VaccinationByAge(SplittableRandom rnd) {
		this.rnd = rnd;
	}

	@Override
	public int handleVaccination(Map<Id<Person>, EpisimPerson> persons, boolean reVaccination, int availableVaccinations, int iteration, double now) {
		if (availableVaccinations == 0)
			return 0;

		// perAge is an ArrayList where we have for each age (in years) an
		// ArrayList of Persons that are qualified for a vaccination
		ArrayList<ArrayList<EpisimPerson>> perAge = new ArrayList<ArrayList<EpisimPerson>>(MAX_AGE);
		for (EpisimPerson p : persons.values()) {
			if (p.getDiseaseStatus() == EpisimPerson.DiseaseStatus.susceptible &&
				p.getVaccinationStatus() == (reVaccination ? EpisimPerson.VaccinationStatus.yes :
											                 EpisimPerson.VaccinationStatus.no) &&
				p.getReVaccinationStatus() == EpisimPerson.VaccinationStatus.no) {
				perAge.get(p.getAge()).add(p);
			}
		}

		int age = MAX_AGE;
		int vaccinationsLeft = availableVaccinations;
		while (vaccinationsLeft > 0) {
			if (perAge.get(age).size() == 0) {
				age--;
				if (age < MINIMUM_AGE_FOR_VACCINATIONS)
					return availableVaccinations - vaccinationsLeft;
				// there are not enough vaccinationsLeft for the Persons of
				// this age, so we shuffle this set for we get the first n Persons
				if (perAge.get(age).size() > vaccinationsLeft)
					Collections.shuffle(perAge.get(age), new Random(EpisimUtils.getSeed(rnd)));
				continue;
			}
			ArrayList<EpisimPerson> candidates = perAge.get(age);
			for (int i = 0; i < candidates.size() && vaccinationsLeft > 0; i++) {
				EpisimPerson person = candidates.get(i);
				vaccinate(person, iteration, reVaccination);
				vaccinationsLeft--;
			}
		}

		return availableVaccinations;
	}
}
