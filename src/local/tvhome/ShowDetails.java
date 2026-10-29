package local.tvhome;

/** A show with its coming and its latest episode; both may be null. */
final class ShowDetails {
    final ShowHit hit;
    final EpisodeInfo next;
    final EpisodeInfo previous;

    ShowDetails(ShowHit hit, EpisodeInfo next, EpisodeInfo previous) {
        this.hit = hit;
        this.next = next;
        this.previous = previous;
    }
}
