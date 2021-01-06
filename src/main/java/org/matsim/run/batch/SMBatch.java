package org.matsim.run.batch;

import com.google.inject.AbstractModule;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.BatchRun;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.EpisimUtils;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.TracingConfigGroup.CapacityType;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.FixedPolicy.ConfigBuilder;
import org.matsim.run.RunParallel;
import org.matsim.run.modules.SnzBerlinProductionScenario;

import javax.annotation.Nullable;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;


/**
 * Interventions for symmetric Berlin week model with different contact models
 */
public class SMBatch implements BatchRun<SMBatch.Params> {

	@Override
	public AbstractModule getBindings(int id, @Nullable Params params) {
		return new SnzBerlinProductionScenario.Builder().setSnapshot( SnzBerlinProductionScenario.Snapshot.no ).createSnzBerlinProductionScenario();
	}

	@Override
	public Metadata getMetadata() {
		return Metadata.of("berlin", "calibration");
	}

	@Override
	public Config prepareConfig(int id, Params params) {

		SnzBerlinProductionScenario module = new SnzBerlinProductionScenario.Builder().setSnapshot(
				SnzBerlinProductionScenario.Snapshot.no ).createSnzBerlinProductionScenario();
		Config config = module.config();
		config.global().setRandomSeed(params.seed);
		
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);
		
//		double thetaFactor = Double.parseDouble(params.weatherMidPoint_Theta.split("_")[1]);
		episimConfig.setCalibrationParameter(episimConfig.getCalibrationParameter() * params.theta);
		episimConfig.setStartDate("2020-02-25");
//		episimConfig.setSnapshotSeed(EpisimConfigGroup.SnapshotSeed.reseed);

		Map<LocalDate, Integer> importMap = new HashMap<>();
		int importOffset = 0;
//		importMap.put(episimConfig.getStartDate(), Math.max(1, (int) Math.round(0.9 * 1)));
		importMap = SnzBerlinProductionScenario.interpolateImport(importMap, params.importFactorSpring, LocalDate.parse("2020-02-24").plusDays(importOffset),
				LocalDate.parse("2020-03-09").plusDays(importOffset), 0.9, 23.1);
		importMap = SnzBerlinProductionScenario.interpolateImport(importMap, params.importFactorSpring, LocalDate.parse("2020-03-09").plusDays(importOffset),
				LocalDate.parse("2020-03-23").plusDays(importOffset), 23.1, 3.9);
		importMap = SnzBerlinProductionScenario.interpolateImport(importMap, params.importFactorSpring, LocalDate.parse("2020-03-23").plusDays(importOffset),
				LocalDate.parse("2020-04-13").plusDays(importOffset), 3.9, 0.1);
		
		if (params.importFactorAfterJune == 0.) {
			importMap.put(LocalDate.parse("2020-06-08"), 1);
		}
		else {
			importMap = SnzBerlinProductionScenario.interpolateImport(importMap, params.importFactorAfterJune, LocalDate.parse("2020-06-08").plusDays(importOffset),
					LocalDate.parse("2020-07-13").plusDays(importOffset), 0.1, 2.7);
			importMap = SnzBerlinProductionScenario.interpolateImport(importMap, params.importFactorAfterJune, LocalDate.parse("2020-07-13").plusDays(importOffset),
					LocalDate.parse("2020-08-10").plusDays(importOffset), 2.7, 17.9);
			importMap = SnzBerlinProductionScenario.interpolateImport(importMap, params.importFactorAfterJune, LocalDate.parse("2020-08-10").plusDays(importOffset),
					LocalDate.parse("2020-09-07").plusDays(importOffset), 17.9, 5.4);
		}
		episimConfig.setInfections_pers_per_day(importMap);	

		try {
//			double weatherMidPont = Double.parseDouble(params.weatherMidPoint_Theta.split("_")[0]);
//			Map<LocalDate, Double> outdoorFractions = EpisimUtils.getOutdoorFractionsFromWeatherData("berlinWeather.csv", params.rainThreshold, params.weatherMidPoint -params.weatherSlope, params.weatherMidPoint +params.weatherSlope );

			double weatherMidPointSpring = Double.parseDouble( params.weatherMidPoints.split( "_" )[0] );
			double weatherMidPointFall = Double.parseDouble( params.weatherMidPoints.split( "_" )[1] );
			Map<LocalDate, Double> outdoorFractions = EpisimUtils.getOutdoorFractions2( SnzBerlinProductionScenario.INPUT.resolve("berlinWeather.csv").toFile(), params.rainThreshold, weatherMidPointSpring, weatherMidPointFall,
					(double) params.weatherSlope );
			episimConfig.setLeisureOutdoorFraction(outdoorFractions);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (params.childSusInf.equals("0")) {
			episimConfig.setAgeInfectivity(Map.of(
					24, 0.,
					25, 1d
					));
			episimConfig.setAgeSusceptibility(Map.of(
					24, 0.,
					25, 1d
					));
		}
		if (params.childSusInf.equals("linear")) {
			episimConfig.setAgeInfectivity(Map.of(
					0, 0.,
					20, 1d
					));
			episimConfig.setAgeSusceptibility(Map.of(
					0, 0.7,
					20, 1d
					));
		}
		
		episimConfig.setHospitalFactor(params.hospitalFactor);
		
		episimConfig.setSnapshotInterval(90);
		
		ConfigBuilder builder = FixedPolicy.parse(episimConfig.getPolicy());
		
		builder.apply("2020-08-20", "2021-12-31", (d, e) -> e.put("fraction", 1 - params.leisureFactor * (1 - (double) e.get("fraction"))) , "leisure");
		
		episimConfig.setPolicy(FixedPolicy.class, builder.build());
		
		TracingConfigGroup tracingConfig = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class);
		
		tracingConfig.setCapacityType(CapacityType.PER_PERSON);

		tracingConfig.setTracingCapacity_pers_per_day(Map.of(
				LocalDate.of(2020, 4, 1), params.tracingCapacitySpring,
				LocalDate.of(2020, 6, 15), params.tracingCapacityAfterJune
		));
		
		

		return config;
	}

	public static final class Params {

		@GenerateSeeds(2)
		public long seed;
		
		@Parameter({0.7, 0.75, 0.8, 0.85})
		double theta;
		
		@Parameter({1., 1.25, 1.5, 2.})
		double leisureFactor;
		
		@Parameter({0.5})
		double rainThreshold;
		
		@Parameter({4.})
		double importFactorSpring;
		
		@Parameter({0.})
		double importFactorAfterJune;

		@StringParameter( {"17.5_22.5","17.5_25.0","20.0_25.0"}) 
		String weatherMidPoints ;

//		@Parameter({25.})
//		double weatherMidPoint;
//
		@Parameter({0.5})
		double hospitalFactor;
		
		@IntParameter({5})
		int weatherSlope;
		
//		@StringParameter({
//			"17.5_0.7", "17.5_0.75", "17.5_0.8", "17.5_0.85",
//			"20.0_0.65", "20.0_0.7", "20.0_0.75", "20.0_0.8",
//			"22.5_0.6", "22.5_0.65", "22.5_0.7", "22.5_0.75",
//			"25.0_0.55", "25.0_0.6", "25.0_0.65", "25.0_0.7"})
//		public String weatherMidPoint_Theta;
		
//		@StringParameter({"0", "current", "linear"})
		@StringParameter({"current"})
		public String childSusInf;
		
		@IntParameter({0, 25})
		int tracingCapacitySpring;
		
		@IntParameter({0, 150, 250})
		int tracingCapacityAfterJune;
		

	}

	public static void main( String[] args ){
		String [] args2 = {
				RunParallel.OPTION_SETUP, SMBatch.class.getName(),
				RunParallel.OPTION_PARAMS, Params.class.getName(),
				RunParallel.OPTION_THREADS, Integer.toString( 4 ),
				RunParallel.OPTION_ITERATIONS, Integer.toString( 330 ),
				RunParallel.OPTION_METADATA
		};

		RunParallel.main( args2 );
	}


}

