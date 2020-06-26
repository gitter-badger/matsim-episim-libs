package org.matsim.run.batch;

import com.google.inject.AbstractModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.episim.BatchRun;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import org.matsim.run.modules.SnzBerlinScenario25pct2020;

import javax.annotation.Nullable;
import java.util.Map;


/**
 * This batch run explores restrictions on group sizes of activities.
 */
public class RestrictGroupSizes implements BatchRun<RestrictGroupSizes.Params> {


	@Override
	public AbstractModule getBindings(int id, @Nullable Object params) {
		return new SnzBerlinScenario25pct2020();
	}

	@Override
	public Metadata getMetadata() {
		return Metadata.of("berlin", "groupSizes");
	}

	@Override
	public Config prepareConfig(int id, Params params) {

		SnzBerlinScenario25pct2020 module = new SnzBerlinScenario25pct2020();
		Config config = module.config();

		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		FixedPolicy.ConfigBuilder builder = FixedPolicy.parse(episimConfig.getPolicy());

		builder.clearAfter("2020-03-07", "leisure");


		if (params.bySize.equals("yes")) {

			Map<Double, Integer> sizes = Map.of(
					0.10, 32,
					0.25, 60,
					0.50, 104,
					0.75, 196,
					0.90, 396
			);

			builder.restrict("2020-03-07", Restriction.ofGroupSize(sizes.get(params.remaining)), "leisure");

		} else {

			builder.restrict("2020-03-07", Restriction.of(params.remaining), "leisure");

		}

		episimConfig.setPolicy(FixedPolicy.class, builder.build());

		return config;
	}

	public static final class Params {

		@Parameter({0.1, 0.25, 0.5, 0.75, 0.9})
		double remaining;

		@StringParameter({"yes", "no"})
		String bySize;

	}

}
